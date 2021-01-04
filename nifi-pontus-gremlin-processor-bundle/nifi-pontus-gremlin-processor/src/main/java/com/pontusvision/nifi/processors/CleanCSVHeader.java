/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pontusvision.nifi.processors;

import com.pontusvision.utils.StringReplacer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Leo Martins
 */
@Tags({ "Clea", "CSV" , "Format", "Headers"}) @CapabilityDescription("Clean up CSV file headers.")

public class CleanCSVHeader extends AbstractProcessor
{
  public enum MULTIVALUE_HEADER_STRATEGY {
    CONCAT,
    REPLACE,
    COL_NUM;

    public final static String[] strValues =
            EnumSet.allOf(MULTIVALUE_HEADER_STRATEGY.class).stream().map(e -> e.name()).collect(Collectors.toList()).
                    toArray(new String[0]);


    }
  private List<PropertyDescriptor> properties;

  private Set<Relationship> relationships;
  String  findText      = null;
  String  replaceText   = "_";
  String  prefixText    = "pg_";
  Boolean useRegex      = false;
  Boolean removeAccents = true;
  char  csvDelimiter  = ',';
  String  recordSeparator  = "\n";
  Integer numHeadersToMerge  = 1;
  MULTIVALUE_HEADER_STRATEGY multiHeaderMergeStrategy = MULTIVALUE_HEADER_STRATEGY.REPLACE;

  public static final String MATCH_ATTR = "match";

  final static PropertyDescriptor CSV_FIND_TEXT = new PropertyDescriptor.Builder()
      .name("Text to find (in the header)").defaultValue(".").required(false)
      .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

  final static PropertyDescriptor CSV_REPLACE_TEXT = new PropertyDescriptor.Builder()
      .name("Text to replace (in the header)")
      .description("Text to replace (in the header)")
      .addValidator(new StandardValidators.StringLengthValidator(0,10000000))
      .defaultValue("_").required(true).build();

  final static PropertyDescriptor USE_REGEX = new PropertyDescriptor.Builder()
      .name("Use Regex").defaultValue("false").required(true)
      .addValidator(StandardValidators.BOOLEAN_VALIDATOR).build();

  final static PropertyDescriptor REMOVE_ACCENTS = new PropertyDescriptor.Builder()
      .name("Remove Accents").defaultValue("true").required(true)
      .description("Removes latin accents and replaces with normal vars.")
      .addValidator(StandardValidators.BOOLEAN_VALIDATOR).build();

  final static PropertyDescriptor CSV_REPLACEMENT_PREFIX = new PropertyDescriptor.Builder()
      .name("Replacement Prefix").defaultValue("pg_").required(true)
      .addValidator(new StandardValidators.StringLengthValidator(0, 1000)).build();

  final static PropertyDescriptor CSV_DELIMITER = new PropertyDescriptor.Builder()
      .name("CSV Delimiter").description("CSV Delimiter").defaultValue(",").required(false)
      .addValidator(new StandardValidators.StringLengthValidator(1,1)).build();

  final static PropertyDescriptor RECORD_SEPARATOR = new PropertyDescriptor.Builder()
          .name("Record Separator").defaultValue("\n").required(false)
          .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

  final static PropertyDescriptor MULTI_LINE_HEADER_COUNT = new PropertyDescriptor.Builder()
          .name("Number Column Headers to merge")
          .description("Number Column Headers to merge (using the Multi line header merge strategy")
          .defaultValue("1").required(false)
          .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR).build();
  final static PropertyDescriptor MULTI_LINE_HEADER_MERGE_STRATEGY = new PropertyDescriptor.Builder()
          .name("Strategy to merge multiple headers")
          .description("Strategy to merge multiple headers (only applicable if the multiline header count is > 1):\n" +
                  " CONCAT (using =A2&\" \"&A3&\" \"&A4), or" +
                  " REPLACE (if (A2!=\"\",A2,if (A3!=\"\", A3, A4)))")
          .allowableValues(MULTIVALUE_HEADER_STRATEGY.strValues)
          .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
          .defaultValue("REPLACE").required(false).build();


  final static CSVFormat getCSVFormatFromString (String input){
    if ("DEFAULT".equalsIgnoreCase(input)|| "CUSTOM".equalsIgnoreCase(input)){
      return CSVFormat.DEFAULT;
    }
    else if ("MONGODB_CSV".equalsIgnoreCase(input)){
      return CSVFormat.MONGODB_CSV;
    }
    else if ("TDF".equalsIgnoreCase(input)){
      return CSVFormat.TDF;
    }
    else if ("POSTGRESQL_TEXT".equalsIgnoreCase(input)){
      return CSVFormat.POSTGRESQL_TEXT;
    }
    else if ("POSTGRESQL_CSV".equalsIgnoreCase(input)){
      return CSVFormat.POSTGRESQL_CSV;
    }
    else if ("ORACLE".equalsIgnoreCase(input)){
      return CSVFormat.ORACLE;
    }
    else if ("RFC4180".equalsIgnoreCase(input)){
      return CSVFormat.RFC4180;
    }
    else if ("MYSQL".equalsIgnoreCase(input)){
      return CSVFormat.MYSQL;
    }
    else if ("INFORMIX_UNLOAD_CSV".equalsIgnoreCase(input)){
      return CSVFormat.INFORMIX_UNLOAD_CSV;
    }
    else if ("INFORMIX_UNLOAD".equalsIgnoreCase(input)){
      return CSVFormat.INFORMIX_UNLOAD;
    }
    else if ("EXCEL".equalsIgnoreCase(input)) {
      return CSVFormat.EXCEL;
    }
    return null;

  }

  final  static String [] allowedCSVFormatVals = {"CUSTOM","DEFAULT","EXCEL","INFORMIX_UNLOAD",
        "INFORMIX_UNLOAD_CSV","INFORMIX_UNLOAD_CSV","MYSQL","RFC4180","ORACLE","POSTGRESQL_CSV","POSTGRESQL_TEXT",
        "TDF"};

  final static Validator CSV_PROCESSOR_FORMAT_VALIDATOR = (subject, input, context) -> {
    ValidationResult.Builder builder = new ValidationResult.Builder();
    builder.subject(subject).input(input);
    if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(input)) {
      return (new ValidationResult.Builder()).subject(subject).input(input).explanation("Expression Language Present")
              .valid(true).build();
    } else {
      try {
        if (getCSVFormatFromString(input) == null) {
          return (new ValidationResult.Builder()).subject(subject).input(input).explanation("Invalid CSVFormat name")
                  .valid(false).build();
        }
        builder.valid(true);

      } catch (IllegalArgumentException var6) {
        builder.valid(false).explanation(var6.getMessage());
      }

      return builder.build();
    }
  };



  final static PropertyDescriptor CSV_PROCESSOR_FORMAT = new PropertyDescriptor.Builder()
          .name("CSV Format").defaultValue("CUSTOM").description("The CSV format used").required(false).allowableValues(allowedCSVFormatVals)
          .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
          .addValidator(CSV_PROCESSOR_FORMAT_VALIDATOR).build();


  public static final Relationship SUCCESS = new Relationship.Builder().name("SUCCESS")
                                                                       .description("Success relationship").build();

  public static final Relationship FAILURE   = new Relationship.Builder().name("FAILURE")
                                                                         .description("Failure relationship").build();

  @Override public void init(final ProcessorInitializationContext context)
  {
    List<PropertyDescriptor> properties = new ArrayList<>();
    properties.add(CSV_FIND_TEXT);
    properties.add(CSV_REPLACE_TEXT);
    properties.add(REMOVE_ACCENTS);
    properties.add(USE_REGEX);
    properties.add(CSV_REPLACEMENT_PREFIX);
    properties.add(CSV_DELIMITER);
    properties.add(RECORD_SEPARATOR);
    properties.add(MULTI_LINE_HEADER_COUNT);
    properties.add(MULTI_LINE_HEADER_MERGE_STRATEGY);
    properties.add(CSV_PROCESSOR_FORMAT);

    this.properties = Collections.unmodifiableList(properties);

    Set<Relationship> relationships = new HashSet<>();
    relationships.add(FAILURE);
    relationships.add(SUCCESS);
    this.relationships = Collections.unmodifiableSet(relationships);
  }

  private static final int BUFFER_SIZE = 2 * 1024 * 1024;

  private void copy(InputStream input, int offset, OutputStream output) throws IOException
  {
    byte[] buffer    = new byte[BUFFER_SIZE];
    int    bytesRead = input.read(buffer);
    while (bytesRead != -1)
    {
      output.write(buffer, 0, bytesRead);
      bytesRead = input.read(buffer);
    }
    //If needed, close streams.

  }

  public CSVFormat getCsvFormat(final ProcessContext context, final FlowFile flowFile){
    String input = context.getProperty("CSV_PROCESSOR_FORMAT").evaluateAttributeExpressions(flowFile).getValue();
    CSVFormat csvFormat = getCSVFormatFromString(input);
    if ("CUSTOM".equalsIgnoreCase(input)){
      csvFormat = csvFormat.withDelimiter(this.csvDelimiter)
               .withRecordSeparator(this.recordSeparator)
               .withEscape('\\');

    }

    return csvFormat;
  }

  @Override public void onPropertyModified(final PropertyDescriptor descriptor, final String oldValue,
                                           final String newValue)
  {

    if (descriptor.equals(CSV_FIND_TEXT))
    {
      findText = newValue;
    }
    else if (descriptor.equals(CSV_REPLACE_TEXT))
    {
      replaceText = newValue;
    }
    else if (descriptor.equals(USE_REGEX))
    {
      useRegex = Boolean.parseBoolean(newValue);
    }
    else if (descriptor.equals(REMOVE_ACCENTS))
    {
      removeAccents = Boolean.parseBoolean(newValue);
    }
    else if (descriptor.equals(CSV_DELIMITER))
    {
      csvDelimiter = newValue.charAt(0);
    }
    else if (descriptor.equals(RECORD_SEPARATOR))
    {
      recordSeparator = newValue;
    }
    else if (descriptor.equals(CSV_REPLACEMENT_PREFIX))
    {
      prefixText = newValue;
    }
    else if (descriptor.equals(MULTI_LINE_HEADER_COUNT))
    {
      numHeadersToMerge = Integer.parseInt(newValue);
    }
    else if (descriptor.equals(MULTI_LINE_HEADER_MERGE_STRATEGY))
    {
      multiHeaderMergeStrategy = MULTIVALUE_HEADER_STRATEGY.valueOf(newValue);
    }

  }
  public String cleanHeaders (String headerSub){
    if (useRegex)
    {
      headerSub = Pattern.compile(findText, Pattern.DOTALL).matcher(headerSub).replaceAll(replaceText);

     // headerSub = headerSub.replaceAll(findText, replaceText);
    }
    else
    {
      headerSub = StringReplacer.replaceAll(headerSub, (findText), replaceText);
    }
    if (removeAccents)
    {
      headerSub = java.text.Normalizer.normalize(headerSub,  java.text.Normalizer.Form.NFD);
      //string = string.replaceAll("[^\\p{ASCII}]", "");
      headerSub = headerSub.replaceAll("\\p{M}", "");

    }

    if (StringUtils.isNotEmpty(prefixText))
    {
      StringBuffer sb      = new StringBuffer(prefixText);
      sb.append(headerSub);

      headerSub = sb.toString();

    }
    return headerSub;
  }
  public String readHeaders(BufferedReader reader, CSVFormat format) throws IOException {

    format = format.withAllowMissingColumnNames();

    CSVParser parser = new CSVParser(reader, format);

    Iterator<CSVRecord> recordIterator = parser.iterator();

    List<CSVRecord> headers = new ArrayList<>();
    int counter = 0;

    int maxEntries = 0;

    while (recordIterator.hasNext() && counter < numHeadersToMerge){
      CSVRecord record = recordIterator.next();
      headers.add(record);
      maxEntries = Math.max(maxEntries,record.size());
      counter ++;
    }

    StringBuffer strbuf = new StringBuffer();
    if (multiHeaderMergeStrategy == MULTIVALUE_HEADER_STRATEGY.CONCAT) {
      for (int i = 0; i < maxEntries; i++) {
        if (i != 0){
          strbuf.append(this.csvDelimiter);
        }

        for (int j = 0; j < this.numHeadersToMerge; j++) {
          try {
            String headerVal = headers.get(j).get(i);
            if (headerVal.length() > 0) {
              headerVal = cleanHeaders(headerVal);
              strbuf.append(headerVal);
            }

          } catch (Throwable t) {
            // ignore
          }

        }
      }
    }
    else if (multiHeaderMergeStrategy == MULTIVALUE_HEADER_STRATEGY.REPLACE){
      for (int i = 0; i < maxEntries; i++) {
        if (i != 0){
          strbuf.append(this.csvDelimiter);
        }
        for (int j = this.numHeadersToMerge - 1; j >= 0; j--) {
          try {
            String headerVal = headers.get(j).get(i);
            if (headerVal.length() > 0) {
              headerVal = cleanHeaders(headerVal);
              strbuf.append(headerVal);
              break;
            }

          } catch (Throwable t) {
            // ignore
          }

        }
      }

    }
    else if (multiHeaderMergeStrategy == MULTIVALUE_HEADER_STRATEGY.COL_NUM){
      for (int i = 0; i < maxEntries; i++) {
        if (i != 0){
          strbuf.append(this.csvDelimiter);
        }
        if (StringUtils.isNotEmpty(prefixText)){
          strbuf.append(prefixText);
        }
        strbuf.append(i+1);
      }

    }

    return strbuf.toString();

  }

  @Override public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException
  {
    final ComponentLog log      = this.getLogger();
    final FlowFile     origFlowfile = session.get();


    if (origFlowfile == null){
      return;
    }

    final FlowFile flowfile = session.clone(origFlowfile);

    session.remove(origFlowfile);

    final CSVFormat currCsvFormat = this.getCsvFormat(context, flowfile);

    session.read(flowfile, in -> {
      FlowFile ffile = session.create();
      ffile = session.putAllAttributes(ffile, flowfile.getAttributes());

      try
      {

        //          FlowFile ffile = flowfile;
        //          LineIterator li = IOUtils.lineIterator(in, Charset.defaultCharset().toString());
        //
        //          String header = li.nextLine();

        //          ffile = session.write(ffile, out -> out.write(headerBytes));

//        FlowFile ffile = session.create();
//        ffile = session.putAllAttributes(ffile, flowfile.getAttributes());

        session.write(ffile, out -> {
          BufferedReader reader = new BufferedReader(new InputStreamReader(in));

          if (!reader.ready()){
            return;
          }

          String headerSub = readHeaders(reader,currCsvFormat);

          in.reset();
          for(int i = 0; i < numHeadersToMerge; i ++){
            reader.readLine();
          }


          StringBuffer sb = new StringBuffer();

          sb.append(headerSub).append(recordSeparator);
          while (reader.ready()){

            sb.append(reader.readLine()).append(recordSeparator);
          }
          final byte[] data = sb.toString().getBytes(Charset.defaultCharset());

          out.write(data);

        });

        session.transfer(ffile, SUCCESS);
//        session.remove(flowfile);

      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        log.error("Failed to read json string.");
        session.transfer(ffile, FAILURE);
//        session.remove(flowfile);

      }
    });

//    session.remove(flowfile);
    //    session.commit();

  }

  @Override public Set<Relationship> getRelationships()
  {
    return relationships;
  }

  @Override public List<PropertyDescriptor> getSupportedPropertyDescriptors()
  {
    return properties;
  }

}

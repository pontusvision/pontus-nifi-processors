/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pontusvision.nifi.processors;

import com.pontusvision.utils.StringReplacer;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.*;

/**
 * @author Leo Martins
 */
@Tags({ "JSON", "CSV" }) @CapabilityDescription("Convert CSV to JSON.")

public class CleanCSVHeader extends AbstractProcessor
{

  private List<PropertyDescriptor> properties;

  private Set<Relationship> relationships;
  String  findText      = null;
  String  replaceText   = "_";
  String  prefixText    = "";
  Boolean useRegex      = false;
  Boolean removeAccents = true;
  String  csvDelimiter  = ",";

  public static final String MATCH_ATTR = "match";

  final static PropertyDescriptor CSV_FIND_TEXT = new PropertyDescriptor.Builder()
      .name("Text to find (in the first line)").defaultValue(".").required(false)
      .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

  final static PropertyDescriptor CSV_REPLACE_TEXT = new PropertyDescriptor.Builder()
      .name("Text to replace (in the first line)").defaultValue("_").required(true)
      .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

  final static PropertyDescriptor USE_REGEX = new PropertyDescriptor.Builder()
      .name("Use Regex").defaultValue("false").required(true)
      .addValidator(StandardValidators.BOOLEAN_VALIDATOR).build();

  final static PropertyDescriptor REMOVE_ACCENTS = new PropertyDescriptor.Builder()
      .name("Remove Accents").defaultValue("true").required(true)
      .addValidator(StandardValidators.BOOLEAN_VALIDATOR).build();

  final static PropertyDescriptor CSV_REPLACEMENT_PREFIX = new PropertyDescriptor.Builder()
      .name("Replacement Prefix").defaultValue("pg_").required(true)
      .addValidator(new StandardValidators.StringLengthValidator(0, 1000)).build();

  final static PropertyDescriptor CSV_DELIMITER = new PropertyDescriptor.Builder()
      .name("CSV Delimiter").defaultValue(",").required(true)
      .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

  public static final Relationship SUCCESS = new Relationship.Builder().name("SUCCESS")
                                                                       .description("Success relationship").build();

  public static final Relationship FAILURE   = new Relationship.Builder().name("FAILURE")
                                                                         .description("Failure relationship").build();
  public static final Relationship NEED_MORE = new Relationship.Builder().name("NEED_MORE").description(
      "Need More relationship, used if the header is being read and we need more data to process the next record")
                                                                         .build();

  @Override public void init(final ProcessorInitializationContext context)
  {
    List<PropertyDescriptor> properties = new ArrayList<>();
    properties.add(CSV_FIND_TEXT);
    properties.add(CSV_REPLACE_TEXT);
    properties.add(REMOVE_ACCENTS);
    properties.add(USE_REGEX);
    properties.add(CSV_REPLACEMENT_PREFIX);
    properties.add(CSV_DELIMITER);

    this.properties = Collections.unmodifiableList(properties);

    Set<Relationship> relationships = new HashSet<>();
    relationships.add(FAILURE);
    relationships.add(SUCCESS);
    relationships.add(NEED_MORE);
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
      csvDelimiter = newValue;
    }

    else if (descriptor.equals(CSV_REPLACEMENT_PREFIX))
    {
      prefixText = newValue;
    }

  }

  @Override public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException
  {
    final ComponentLog log      = this.getLogger();
    final FlowFile     flowfile = session.get();

    session.read(flowfile, new InputStreamCallback()
    {
      @Override public void process(InputStream in) throws IOException
      {
        try
        {

          //          FlowFile ffile = flowfile;
          //          LineIterator li = IOUtils.lineIterator(in, Charset.defaultCharset().toString());
          //
          //          String header = li.nextLine();

          //          ffile = session.write(ffile, out -> out.write(headerBytes));

          FlowFile ffile = session.create();
          ffile = session.putAllAttributes(ffile, flowfile.getAttributes());

          session.write(ffile, new OutputStreamCallback()
          {
            @Override public void process(OutputStream out) throws IOException
            {

              StringBuffer strbuf = new StringBuffer();

              byte val = -1;
              do
              {
                val = (byte) in.read();
                if (val >= 0)
                {
                  strbuf.append((char) val);
                }

              } while (val != '\n' && val != -1);

              String headerSub = null;
              if (useRegex)
              {
                headerSub = strbuf.toString().replaceAll(findText, replaceText);
              }
              else
              {
                headerSub = StringReplacer.replaceAll(strbuf.toString(), (findText), replaceText);
              }
              if (removeAccents)
              {
                headerSub = Normalizer.normalize(headerSub, Normalizer.Form.NFD);
                headerSub = headerSub.replaceAll("\\p{M}", "");
              }

              if (StringUtils.isNotEmpty(prefixText))
              {
                String[]     headers = headerSub.split(csvDelimiter);
                StringBuffer sb      = new StringBuffer();
                for (int i = 0, ilen = headers.length; i < ilen; i++)
                {
                  if (i != 0)
                  {
                    sb.append(csvDelimiter);
                  }
                  sb.append(prefixText).append(headers[i].trim());
                }
                sb.append("\n");
                headerSub = sb.toString();

              }

              final byte[] headerBytes = headerSub.getBytes();

              out.write(headerBytes);
              //              out.write("\n".getBytes());
              copy(in, headerBytes.length + 1, out);
              //              in.close();
              //              out.close();

            }
          });

          session.transfer(ffile, SUCCESS);

        }
        catch (Exception ex)

        {
          ex.printStackTrace();
          log.error("Failed to read json string.");
          session.transfer(flowfile, FAILURE);
        }
      }
    });

    session.remove(flowfile);
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

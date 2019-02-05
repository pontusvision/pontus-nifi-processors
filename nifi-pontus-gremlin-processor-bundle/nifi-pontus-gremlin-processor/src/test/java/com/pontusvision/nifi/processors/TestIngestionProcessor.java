package com.pontusvision.nifi.processors;

import com.jayway.jsonpath.JsonPath;
import com.pontusvision.utils.LocationAddress;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.csv.CSVReader;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import javax.script.Bindings;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestIngestionProcessor
{

  private static final String TEST_DATA_RESOURCE_DIR = "csv-data/";

  protected static File DEFAULT_INSTALL_DIR = new File(System.getProperty("java.io.tmpdir"), "data-dir");
  protected TestRunner runner;
  protected TestRunner runnerBr;
  PontusTinkerPopClient ptpc;
  PontusTinkerPopClientRecordBulk ptpcBr;
  PropertyDescriptor embeddedServer;
  PropertyDescriptor confURI;
  PropertyDescriptor RECORD_READER;
  PropertyDescriptor query;
  protected String queryStr = "1+1";

  EmbeddedElastic es;

  //      final String queryStr = ""
  //          + "StringBuilder sb = new StringBuilder(); \n"
  //          + "try {\n"
  //          + "    ingestPole(pg_poleJsonStr,graph,g,sb); \n "
  //          + "}catch (Throwable t){\n"
  //          + "    String stackTrace =
  // org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t)\n"
  //          + "\n"
  //          + "    sb.append(\"\\n$t\\n$stackTrace\")\n"
  //          + "}\n"
  //          + "sb.toString()\n";

  public static void prepareAddressParserDir() throws IOException
  {
    System.setProperty(LocationAddress.ADDRESS_PARSER_DIR_OPT,
        DEFAULT_INSTALL_DIR.getAbsolutePath() + File.separator + "datadir" + File.separator + "libpostal");

    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.startsWith("win"))
    {
      System.setProperty("os.arch", "x86");

    }

    if (!DEFAULT_INSTALL_DIR.exists())
    {
      FileUtils.forceMkdir(DEFAULT_INSTALL_DIR);

      File outFile = File.createTempFile("datadir", ".tar.gz", DEFAULT_INSTALL_DIR);
      OutputStream out = new FileOutputStream(outFile);
      final int BUFFER_SIZE = 256 * 1024;
      byte[] buf = new byte[BUFFER_SIZE];

      File folder = new File(".." + File.separator + ".." + File.separator + "pontus-gdpr-graph");
      FileFilter filter = new PrefixFileFilter("datadir.tar.gz-");
      File[] listOfFiles = folder.listFiles(filter);

      for (File file : listOfFiles)
      {
        InputStream in = new FileInputStream(file);
        int b = 0;
        while ((b = in.read(buf)) >= 0)
        {
          out.write(buf, 0, b);
          out.flush();
        }
      }
      out.close();

      InputStream tarGzFile = new FileInputStream(outFile);

      GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(tarGzFile);
      try (TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn))
      {
        TarArchiveEntry entry;

        while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null)
        {
          /** If the entry is a directory, create the directory. **/
          if (entry.isDirectory())
          {
            File f = new File(DEFAULT_INSTALL_DIR, entry.getName());
            boolean created = f.mkdir();
            if (!created)
            {
              System.out.printf("Unable to create directory '%s', during extraction of archive contents.\n",
                  f.getAbsolutePath());
            }
          }
          else
          {
            int count;
            //            byte data[] = new byte[BUFFER_SIZE];
            File tmpOutFile = new File(DEFAULT_INSTALL_DIR, entry.getName());

            FileOutputStream fos = new FileOutputStream(tmpOutFile, false);
            try (BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE))
            {
              while ((count = tarIn.read(buf, 0, BUFFER_SIZE)) != -1)
              {
                dest.write(buf, 0, count);
              }
            }
          }
        }

        System.out.println("Untar completed successfully!");
      }

    }

    com.pontusvision.utils.LocationAddress.parser.getInstance().parseAddress("Rua 25 Andre Tesch");

  }

  public static EmbeddedElastic runES() throws IOException, InterruptedException
  {
    final EmbeddedElastic embeddedElastic = EmbeddedElastic.builder().withElasticVersion("6.4.0")
        //        .withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9300)
        //        .withSetting(PopularProperties.HTTP_PORT, 9200)
        .withSetting(PopularProperties.CLUSTER_NAME, "my_cluster").withStartTimeout(2, MINUTES)

        //        .withPlugin("analysis-stempel")
        //        .withIndex("cars", IndexSettings.builder()
        //            .withType("car", getSystemResourceAsStream("car-mapping.json"))
        //            .build())
        //        .withIndex("books", IndexSettings.builder()
        //            .withType(PAPER_BOOK_INDEX_TYPE, getSystemResourceAsStream("paper-book-mapping.json"))
        //            .withType("audio_book", getSystemResourceAsStream("audio-book-mapping.json"))
        //            .withSettings(getSystemResourceAsStream("elastic-settings.json"))
        //            .build())
        .build().start();

    return embeddedElastic;
  }

  public static void copyResourceToFile(String resource, String fileName, ClassLoader classLoader) throws IOException
  {

    URL inMemPropsUrl = Thread.currentThread().getContextClassLoader()
        .getResource(resource); // classLoader.getResource(resource);
    ReadableByteChannel rbc = Channels.newChannel(inMemPropsUrl.openStream());
    FileOutputStream fos = new FileOutputStream(fileName);
    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
  }

  /*
   * Create a Tinkerpop Nifi Processor that has an embedded in-memory graph,
   * and a query that invokes the ingestPole() function that dedups any entries
   * within the batch.
   */

  @Before public void setup() throws Exception
  {

    prepareAddressParserDir();

    es = runES();

    ptpc = new PontusTinkerPopClient();

    ptpcBr = new PontusTinkerPopClientRecordBulk();

    RECORD_READER = ptpcBr.getPropertyDescriptor("record-reader");

    embeddedServer = ptpc.getPropertyDescriptor("Tinkerpop Embedded Server");
    confURI = ptpc.getPropertyDescriptor("Tinkerpop Client configuration URI");
    query = ptpc.getPropertyDescriptor("Tinkerpop Query");

    CSVReader service = new org.apache.nifi.csv.CSVReader();

    Map<String, String> controllerSvcProps = new HashMap<>();
    controllerSvcProps.put("schema-access-strategy", "csv-header-derived");
    controllerSvcProps.put("csv-reader-csv-parser", "commons-csv");
    controllerSvcProps.put("Date Format", "dd/MM/yyyy");
    controllerSvcProps.put("CSV Format", "rfc-4180");
    controllerSvcProps.put("Skip Header Line", "true");

    ClassLoader testClassLoader = TestIngestionProcessor.class.getClassLoader();
    URL url = testClassLoader.getResource("graphdb-conf/gremlin-mem.yml");

    runnerBr = TestRunners.newTestRunner(ptpcBr);
    runnerBr.setValidateExpressionUsage(true);
    runnerBr.setProperty(embeddedServer, "true");
    runnerBr.setProperty(confURI, url.toURI().toString());
    runnerBr.setProperty(query, queryStr);
    runnerBr.addControllerService("Demo_CRM_CSVReader", service, controllerSvcProps);

    runnerBr.enableControllerService(service);
    runnerBr.setProperty(RECORD_READER, "Demo_CRM_CSVReader");

    ptpcBr.onPropertyModified(embeddedServer, "true", "true");
    ptpcBr.onPropertyModified(confURI, "", url.toURI().toString());
    ptpcBr.onPropertyModified(query, "true", queryStr);
    ptpcBr.onPropertyModified(RECORD_READER, "", "Demo_CRM_CSVReader");

    runner = TestRunners.newTestRunner(ptpc);
    runner.setValidateExpressionUsage(true);
    runner.setProperty(embeddedServer, "true");
    runner.setProperty(confURI, url.toURI().toString());
    runner.setProperty(query, queryStr);

    ptpc.onPropertyModified(embeddedServer, "true", "true");
    ptpc.onPropertyModified(confURI, "", url.toURI().toString());
    ptpc.onPropertyModified(query, "true", queryStr);

    runner.assertValid();
  }

  //  public void setup2() throws Exception
  //  {
  //    ptpc = new PontusTinkerPopClient();
  //
  //    embeddedServer = ptpc.getPropertyDescriptor("Tinkerpop Embedded Server");
  //    confURI = ptpc.getPropertyDescriptor("Tinkerpop Client configuration URI");
  //    query = ptpc.getPropertyDescriptor("Tinkerpop Query");
  //    ClassLoader testClassLoader = TestIngestionProcessor.class.getClassLoader();
  //    URL url = testClassLoader.getResource("graphdb-conf/gremlin-mem.yml");
  //
  //    runner = TestRunners.newTestRunner(ptpc);
  //    runner.setValidateExpressionUsage(true);
  //    runner.setProperty(embeddedServer, "true");
  //    runner.setProperty(confURI, url.toURI().toString());
  //    runner.setProperty(query, "ingestPoleCreate(pg_poleJsonStr,graph,g)");
  //
  //    ptpc.onPropertyModified(embeddedServer, "true", "true");
  //    ptpc.onPropertyModified(confURI, "", url.toURI().toString());
  //    ptpc.onPropertyModified(query, "true", "ingestPoleCreate(pg_poleJsonStr,graph,g)");
  //
  //    runner.assertValid();
  //  }

  @Test public void testBatchNormalOrder() throws Exception
  {
    testBatchCommon("phase1.csv");
  }

  //  @Test public void testBatchReverseOrder() throws Exception
  //  {
  //    testBatchCommon("pole-batch-reverse-order.json");
  //  }

  //  @Test public void testSpitCreateUpdateBatchNormalOrder() throws Exception
  //  {
  //    testBatchCommonSplitCreateUpdate("pole-batch.json");
  //  }
  //
  //  @Test public void testSpitCreateUpdateBatchReverseOrder() throws Exception
  //  {
  //    testBatchCommonSplitCreateUpdate("pole-batch-reverse-order.json");
  //  }

  @Test public void testBatchEntriesInEventualConsistencyLimbo() throws Exception
  {

    String batchFileName = "pole-batch-matches-and-not-found-matches.json";

    /* Load a batch of 2 requests separated by CDP_DELIMITER into the tinkerpop nifi processor*/
    Map<String, String> attribs = new HashMap<>();
    attribs.put("pg_poleJsonStr",
        IOUtils.toString(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), StandardCharsets.UTF_8));
    attribs.put("pg_lastErrorStr", "");

    runner.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
    runner.run();

    List<MockFlowFile> result = runner.getFlowFilesForRelationship(ptpc.REL_FAILURE);

    /* check that we have a successful result */
    runner.assertAllFlowFilesTransferred(ptpc.REL_FAILURE, 1);

    String data = new String(result.get(0).toByteArray());
    assertNotNull(data);

    /* extract the query results */
    //    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");

    /* Now, verify that the graph itself has the correct data by making a few queries directly to it */

    Bindings bindings = ptpc.getBindings(result.get(0));

    byte[] res = ptpc.runQuery(bindings,
        "g.V().has('P.identity.id',eq('ccac8d5ff3288132af67e98ef771c722cf85e9c44b93eebb1e906646e0054725')).count()");
    String data2 = new String(res);
    Integer numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with GUID that had matched == false", 1, (int) numItemsWithGUID);

    res = ptpc.runQuery(bindings,
        "g.V().has('E.journey.id',eq('bf08c81b6becff33a2478f4d8aff7700a081ec737adc683a9c5078dae2df3d11')).count()");
    data2 = new String(res);
    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with GUID that had matched == false", 1, (int) numItemsWithGUID);

    res = ptpc.runQuery(bindings,
        "g.V().has('O.document.id',eq('2ce842fba7ed428c331e6c156f893aaeaf216b661e03782bc884da36861f981e')).count()");
    data2 = new String(res);
    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with GUID that had matched == false", 1, (int) numItemsWithGUID);

    res = ptpc.runQuery(bindings,
        "g.V().has('L.place.id',eq('39a45fe15843d19277e6e32927cf57ef85b6d4937dd62a6680c099eb03432bf2')).count()");
    data2 = new String(res);
    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with GUID that had matched == true and false", 1, (int) numItemsWithGUID);

    res = ptpc.runQuery(bindings,
        "g.V().has('L.place.id',eq('a1ab8e18efb54371d789de921375354aee750cf6fc97e0af00d30f8b01921dac')).count()");
    data2 = new String(res);
    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("No matches for item with GUID that had matched == true only", 0, (int) numItemsWithGUID);

  }

  public void testCSVRecordsCommon(String batchFileName) throws Exception
  {

    /* Load a batch of 2 requests separated by CDP_DELIMITER into the tinkerpop nifi processor*/
    Map<String, String> attribs = new HashMap<>();
    attribs.put("pg_poleJsonStr",
        IOUtils.toString(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), StandardCharsets.UTF_8));
    attribs.put("pg_lastErrorStr", "");

    runner.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
    runner.run();

    List<MockFlowFile> result = runner.getFlowFilesForRelationship(ptpc.REL_SUCCESS);

    /* check that we have a successful result */
    runner.assertAllFlowFilesTransferred(ptpc.REL_SUCCESS, 1);

    String data = new String(result.get(0).toByteArray());
    assertNotNull(data);

    /* extract the query results */
    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");

    Integer numEntries = JsonPath.read(poleRes, "$.length()");

    assertEquals("Batch count preserved", 2, (int) numEntries);

    Integer numAssocFirstBatch = JsonPath.read(poleRes, "$.[0].numberOfAssociationsCreated");
    Integer numAssocSecondBatch = JsonPath.read(poleRes, "$.[1].numberOfAssociationsCreated");

    assertEquals("Num of Assocs first batch is OK", 9, (int) numAssocFirstBatch);
    assertEquals("Num of Assocs second batch is OK", 9, (int) numAssocSecondBatch);

    numAssocFirstBatch = JsonPath.read(poleRes, "$.[0].associations.length()");
    numAssocSecondBatch = JsonPath.read(poleRes, "$.[1].associations.length()");

    assertEquals("Num of Assocs first batch is OK", 9, (int) numAssocFirstBatch);
    assertEquals("Num of Assocs second batch is OK", 9, (int) numAssocSecondBatch);

    /* Now, verify that the graph itself has the correct data by making a few queries directly to it */

    Bindings bindings = ptpc.getBindings(result.get(0));

    byte[] res = ptpc.runQuery(bindings,
        "g.V().has('O.document.id',eq('b136564aeb57278596ce59ba86056e71768790612c05931f863beffd96999cd3')).count()");
    String data2 = new String(res);
    Integer numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);

    res = ptpc.runQuery(bindings,
        "g.V().has('O.document.id',eq('b136564aeb57278596ce59ba86056e71768790612c05931f863beffd96999cd3')).both().dedup().count()");
    data2 = new String(res);
    Integer numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Num Neighbours", 1, (int) numNeighbours);

    res = ptpc.runQuery(bindings,
        "g.V().has('L.place.id',eq('6130beff7352acd496ecd3fd97ff404d775b0b64fa9785d9649979290ed67e15')).count()");
    data2 = new String(res);
    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);

    res = ptpc.runQuery(bindings,
        "g.V().has('L.place.id',eq('6130beff7352acd496ecd3fd97ff404d775b0b64fa9785d9649979290ed67e15')).both().dedup().count()");
    data2 = new String(res);
    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Num Neighbours ", 1, (int) numNeighbours);

    res = ptpc.runQuery(bindings,
        "g.V().has('E.journey.id',eq('262c57d3f042f5b27ed64533796cf7c218887c484b6a98cac09c64267b25b994')).both().dedup().count()");
    data2 = new String(res);
    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Num Neighbours ", 6, (int) numNeighbours);

    runner.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + "pole-batch.json"), attribs);
    runner.run();

    result = runner.getFlowFilesForRelationship(ptpc.REL_SUCCESS);
    data = new String(result.get(1).toByteArray());
    assertNotNull(data);

    poleRes = JsonPath.read(data, "$.result.data['@value'][0]");

    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[0].action"));
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[1].action"));
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[2].action"));
    /* Commented out for P.identities and E.xxxMessage should always be created, but the test didn't change the guids of them at runtime
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create",
        "C",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[3].action"));*/
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "C",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[4].action"));
    /* Commented out for P.identities and E.xxxMessage should always be created, but the test didn't change the guids of them at runtime
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create",
        "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[5].action"));*/
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[6].action"));

    //    Assert.assertEquals(hashedKeyExpected, result.get(0).getAttribute("kafka_key"));
  }

  public void testBatchCommon(String batchFileName) throws Exception
  {

    /* Load a batch of 2 requests separated by CDP_DELIMITER into the tinkerpop nifi processor*/
    Map<String, String> attribs = new HashMap<>();
    attribs.put("pg_poleJsonStr",
        IOUtils.toString(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), StandardCharsets.UTF_8));
    attribs.put("pg_lastErrorStr", "");

    runnerBr.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
    runnerBr.run();

    List<MockFlowFile> result = runnerBr.getFlowFilesForRelationship(ptpcBr.REL_SUCCESS);

    /* check that we have a successful result */
    runnerBr.assertAllFlowFilesTransferred(ptpcBr.REL_SUCCESS, 1);

    String data = new String(result.get(0).toByteArray());
    assertNotNull(data);

    /* extract the query results */
    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");

    Integer numEntries = JsonPath.read(poleRes, "$.length()");

    //    assertEquals("Batch count preserved", 2, (int) numEntries);
    //
    //    Integer numAssocFirstBatch = JsonPath.read(poleRes, "$.[0].numberOfAssociationsCreated");
    //    Integer numAssocSecondBatch = JsonPath.read(poleRes, "$.[1].numberOfAssociationsCreated");
    //
    //    assertEquals("Num of Assocs first batch is OK", 9, (int) numAssocFirstBatch);
    //    assertEquals("Num of Assocs second batch is OK", 9, (int) numAssocSecondBatch);
    //
    //    numAssocFirstBatch = JsonPath.read(poleRes, "$.[0].associations.length()");
    //    numAssocSecondBatch = JsonPath.read(poleRes, "$.[1].associations.length()");
    //
    //    assertEquals("Num of Assocs first batch is OK", 9, (int) numAssocFirstBatch);
    //    assertEquals("Num of Assocs second batch is OK", 9, (int) numAssocSecondBatch);

    /* Now, verify that the graph itself has the correct data by making a few queries directly to it */

    Bindings bindings = ptpcBr.getBindings(result.get(0));

    byte[] res = ptpcBr.runQuery(bindings,
        "g.V().has('O.document.id',eq('b136564aeb57278596ce59ba86056e71768790612c05931f863beffd96999cd3')).count()");
    String data2 = new String(res);
    Integer numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);

    res = ptpcBr.runQuery(bindings,
        "g.V().has('O.document.id',eq('b136564aeb57278596ce59ba86056e71768790612c05931f863beffd96999cd3')).both().dedup().count()");
    data2 = new String(res);
    Integer numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Num Neighbours", 1, (int) numNeighbours);

    res = ptpcBr.runQuery(bindings,
        "g.V().has('L.place.id',eq('6130beff7352acd496ecd3fd97ff404d775b0b64fa9785d9649979290ed67e15')).count()");
    data2 = new String(res);
    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);

    res = ptpcBr.runQuery(bindings,
        "g.V().has('L.place.id',eq('6130beff7352acd496ecd3fd97ff404d775b0b64fa9785d9649979290ed67e15')).both().dedup().count()");
    data2 = new String(res);
    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Num Neighbours ", 1, (int) numNeighbours);

    res = ptpcBr.runQuery(bindings,
        "g.V().has('E.journey.id',eq('262c57d3f042f5b27ed64533796cf7c218887c484b6a98cac09c64267b25b994')).both().dedup().count()");
    data2 = new String(res);
    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Num Neighbours ", 6, (int) numNeighbours);

    runner.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + "pole-batch.json"), attribs);
    runner.run();

    result = runner.getFlowFilesForRelationship(ptpcBr.REL_SUCCESS);
    data = new String(result.get(1).toByteArray());
    assertNotNull(data);

    poleRes = JsonPath.read(data, "$.result.data['@value'][0]");

    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[0].action"));
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[1].action"));
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[2].action"));
    /* Commented out for P.identities and E.xxxMessage should always be created, but the test didn't change the guids of them at runtime
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create",
        "C",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[3].action"));*/
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "C",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[4].action"));
    /* Commented out for P.identities and E.xxxMessage should always be created, but the test didn't change the guids of them at runtime
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create",
        "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[5].action"));*/
    assertEquals(
        "Action is still create the second time now that we are using the matched flag to define when to create", "U",
        JsonPath.read(poleRes, "$.[0].poleGUIDs[6].action"));

    //    Assert.assertEquals(hashedKeyExpected, result.get(0).getAttribute("kafka_key"));
  }

  //  public void testBatchCommonSplitCreateUpdate(String batchFileName) throws Exception
  //  {
  ////    setup2();
  //    /* Load a batch of 2 requests separated by CDP_DELIMITER into the tinkerpop nifi processor*/
  //    Map<String, String> attribs = new HashMap<>();
  //    attribs.put("pg_poleJsonStr",
  //        IOUtils.toString(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), StandardCharsets.UTF_8));
  //    attribs.put("pg_lastErrorStr", "");
  //
  //    String createQuery = "ingestPoleCreate(pg_poleJsonStr, graph, g)";
  //
  //    runner.setProperty(query, createQuery);
  //    ptpc.onPropertyModified(query, "", createQuery);
  //
  //    runner.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
  //    runner.run();
  //
  //    List<MockFlowFile> result = runner.getFlowFilesForRelationship(ptpc.REL_SUCCESS);
  //
  //    /* check that we have a successful result */
  //    runner.assertAllFlowFilesTransferred(ptpc.REL_SUCCESS, 1);
  //
  //    String data = new String(result.get(0).toByteArray());
  //    assertNotNull(data);
  //
  //    /* extract the query results */
  //    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");
  //
  //    assertEquals("get a OK message", "OK", poleRes);
  //
  //    // At this stage, we should only have vertices created, but no neighbours.
  //    Bindings bindings = ptpc.getBindings(result.get(0));
  //
  //    byte[] res = ptpc.runQuery(bindings,
  //        "g.V().has('O.document.id',eq('b136564aeb57278596ce59ba86056e71768790612c05931f863beffd96999cd3')).count()");
  //    String data2 = new String(res);
  //    Integer numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);
  //
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('L.place.id',eq('2554cc00d58dda38b0f50b86610f45a9fb592415b3ac8a6bbdb80c43b6c89e95')).count()");
  //    data2 = new String(res);
  //    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);
  //
  //
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('O.document.id',eq('b136564aeb57278596ce59ba86056e71768790612c05931f863beffd96999cd3')).both().dedup().count()");
  //    data2 = new String(res);
  //    Integer numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("No edges after the create step", 0, (int) numNeighbours);
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('L.place.id',eq('6130beff7352acd496ecd3fd97ff404d775b0b64fa9785d9649979290ed67e15')).count()");
  //    data2 = new String(res);
  //    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('L.place.id',eq('6130beff7352acd496ecd3fd97ff404d775b0b64fa9785d9649979290ed67e15')).both().dedup().count()");
  //    data2 = new String(res);
  //    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("No edges after the create step ", 0, (int) numNeighbours);
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('E.journey.id',eq('262c57d3f042f5b27ed64533796cf7c218887c484b6a98cac09c64267b25b994')).both().dedup().count()");
  //    data2 = new String(res);
  //    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("No edges after the create step ", 0, (int) numNeighbours);
  //
  //    //    String updateQuery = "StringBuilder sb = new StringBuilder(); \n"
  //    //        + "try { ingestPoleUpdate(pg_poleJsonStr, graph, g, sb)} \n"
  //    //        + "catch (Throwable t){\n"
  //    //        + "  String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);\n"
  //    //        + "   sb.append(stackTrace);\n"
  //    //        + "}\n"
  //    //        + "sb.toString();";
  //
  //    String updateQuery = "ingestPoleUpdate(pg_poleJsonStr, graph, g)";
  //
  //
  //
  //    //
  //    //    runner.setProperty(query, updateQuery);
  //    //    ptpc.onPropertyModified(query, "", updateQuery);
  //    //
  //    //
  //    // check that the value is still in the graph after resetting the updateQuery.
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('L.place.id',eq('2554cc00d58dda38b0f50b86610f45a9fb592415b3ac8a6bbdb80c43b6c89e95')).count()");
  //    data2 = new String(res);
  //    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);
  //
  //
  //
  //    res = ptpc.runQuery(bindings,updateQuery);
  //    data = new String(res);
  //
  //    //
  //    //
  //    //
  //    //    runner.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
  //    //    runner.run();
  //
  //    // check that the value is still in the graph after enqueue / run .
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('L.place.id',eq('2554cc00d58dda38b0f50b86610f45a9fb592415b3ac8a6bbdb80c43b6c89e95')).count()");
  //    data2 = new String(res);
  //    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);
  //
  //
  //    poleRes = JsonPath.read(data, "$.result.data['@value'][0]");
  //
  //    Integer numEntries = JsonPath.read(poleRes, "$.length()");
  //
  //    assertEquals("Batch count preserved", 2, (int) numEntries);
  //
  //    Integer numAssocFirstBatch = JsonPath.read(poleRes, "$.[0].numberOfAssociationsCreated");
  //    Integer numAssocSecondBatch = JsonPath.read(poleRes, "$.[1].numberOfAssociationsCreated");
  //
  //    assertEquals("Num of Assocs first batch is OK", 9, (int) numAssocFirstBatch);
  //    assertEquals("Num of Assocs second batch is OK", 9, (int) numAssocSecondBatch);
  //
  //    numAssocFirstBatch = JsonPath.read(poleRes, "$.[0].associations.length()");
  //    numAssocSecondBatch = JsonPath.read(poleRes, "$.[1].associations.length()");
  //
  //    assertEquals("Num of Assocs first batch is OK", 9, (int) numAssocFirstBatch);
  //    assertEquals("Num of Assocs second batch is OK", 9, (int) numAssocSecondBatch);
  //
  //    /* Now, verify that the graph itself has the correct data by making a few queries directly to it */
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('O.document.id',eq('b136564aeb57278596ce59ba86056e71768790612c05931f863beffd96999cd3')).count()");
  //    data2 = new String(res);
  //    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('O.document.id',eq('b136564aeb57278596ce59ba86056e71768790612c05931f863beffd96999cd3')).both().dedup().count()");
  //    data2 = new String(res);
  //    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Num Neighbours", 1, (int) numNeighbours);
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('L.place.id',eq('6130beff7352acd496ecd3fd97ff404d775b0b64fa9785d9649979290ed67e15')).count()");
  //    data2 = new String(res);
  //    numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Only one item with GUID", 1, (int) numItemsWithGUID);
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('L.place.id',eq('6130beff7352acd496ecd3fd97ff404d775b0b64fa9785d9649979290ed67e15')).both().dedup().count()");
  //    data2 = new String(res);
  //    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Num Neighbours ", 1, (int) numNeighbours);
  //
  //    res = ptpc.runQuery(bindings,
  //        "g.V().has('E.journey.id',eq('262c57d3f042f5b27ed64533796cf7c218887c484b6a98cac09c64267b25b994')).both().dedup().count()");
  //    data2 = new String(res);
  //    numNeighbours = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
  //    assertEquals("Num Neighbours ", 6, (int) numNeighbours);
  //
  //    //
  //    //    assertEquals(
  //    //        "Action is still create the second time now that we are using the matched flag to define when to create", "C",
  //    //        JsonPath.read(poleRes, "$.[0].poleGUIDs[0].action"));
  //    //    assertEquals(
  //    //        "Action is still create the second time now that we are using the matched flag to define when to create", "C",
  //    //        JsonPath.read(poleRes, "$.[0].poleGUIDs[1].action"));
  //    //    assertEquals(
  //    //        "Action is still create the second time now that we are using the matched flag to define when to create", "C",
  //    //        JsonPath.read(poleRes, "$.[0].poleGUIDs[2].action"));
  //    //    /* Commented out for P.identities and E.xxxMessage should always be created, but the test didn't change the guids of them at runtime
  //    //    assertEquals(
  //    //        "Action is still create the second time now that we are using the matched flag to define when to create",
  //    //        "C",
  //    //        JsonPath.read(poleRes, "$.[0].poleGUIDs[3].action"));*/
  //    //    assertEquals(
  //    //        "Action is still create the second time now that we are using the matched flag to define when to create", "C",
  //    //        JsonPath.read(poleRes, "$.[0].poleGUIDs[4].action"));
  //    //    /* Commented out for P.identities and E.xxxMessage should always be created, but the test didn't change the guids of them at runtime
  //    //    assertEquals(
  //    //        "Action is still create the second time now that we are using the matched flag to define when to create",
  //    //        "U",
  //    //        JsonPath.read(poleRes, "$.[0].poleGUIDs[5].action"));*/
  //    //    assertEquals(
  //    //        "Action is still create the second time now that we are using the matched flag to define when to create", "C",
  //    //        JsonPath.read(poleRes, "$.[0].poleGUIDs[6].action"));
  //    //
  //    //    //    Assert.assertEquals(hashedKeyExpected, result.get(0).getAttribute("kafka_key"));
  //  }

}


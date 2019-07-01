package com.pontusvision.nifi.processors;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.csv.CSVReader;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestIngestionProcessorDockerComposeRemoteBase
{

  protected static final String TEST_DATA_RESOURCE_DIR = "csv-data/";

  protected TestRunner runner;
  protected TestRunner runnerBr;
  PontusTinkerPopClient           ptpc;
  PontusTinkerPopClientRecordBulk ptpcBr;

  public String runSimpleQuery(Bindings bindings, String queryStr)
      throws InterruptedException, ExecutionException, URISyntaxException, IOException
  {
    ptpc.useEmbeddedServer = false;
    return new String( ptpc.runQuery(bindings,queryStr));

  }

  public String runBulkRequestQuery(Bindings bindings, String queryStr)
      throws InterruptedException, ExecutionException, URISyntaxException, IOException
  {
    ptpcBr.useEmbeddedServer = false;
    return new String( ptpcBr.runQuery(bindings,queryStr));

  }


  public Bindings getBulkRequestBindings(FlowFile mockFlowFile)
  {
    ptpcBr.queryAttribPrefixStr = ptpcBr.queryAttribPrefixStr == null ? "pg_" : ptpcBr.queryAttribPrefixStr;
    if (mockFlowFile == null)
    {
      return new SimpleBindings();
    }
    return ptpcBr.getBindings(mockFlowFile);

  }

  public Bindings getSimpleBindings(FlowFile mockFlowFile)
  {
    ptpc.queryAttribPrefixStr = ptpc.queryAttribPrefixStr == null ? "pg_" : ptpc.queryAttribPrefixStr;
    if (mockFlowFile == null)
    {
      return new SimpleBindings();
    }
    return ptpc.getBindings(mockFlowFile);

  }


  public void setBulkRecordClientQuery(String query)
  {
    runnerBr.setProperty("Tinkerpop Query", query);
    ptpcBr.queryStr = query;

  }

  public void setClientQuery(String query)
  {
    runner.setProperty("Tinkerpop Query", query);
    ptpc.queryStr = query;

  }

  public void setupTinkerpopBulkRecordClient() throws Exception
  {

    ptpcBr = new PontusTinkerPopClientRecordBulk();

    ptpcBr.enableStopDuringTestsHook = false;

    PropertyDescriptor PROP_IS_EMBEDDED_SERVER;
    PropertyDescriptor PROP_CONF_URI;
    PropertyDescriptor PROP_RECORD_READER;

    PROP_RECORD_READER = ptpcBr.getPropertyDescriptor("record-reader");

    PROP_IS_EMBEDDED_SERVER = ptpcBr.getPropertyDescriptor("Tinkerpop Embedded Server");
    PROP_CONF_URI = ptpcBr.getPropertyDescriptor("Tinkerpop Client configuration URI");

    CSVReader service = new CSVReader();

    Map<String, String> controllerSvcProps = new HashMap<>();
    controllerSvcProps.put("schema-access-strategy", "csv-header-derived");
    controllerSvcProps.put("csv-reader-csv-parser", "commons-csv");
    controllerSvcProps.put("Date Format", "dd/MM/yyyy");
    controllerSvcProps.put("CSV Format", "rfc-4180");
    controllerSvcProps.put("Skip Header Line", "true");

    ClassLoader testClassLoader = TestIngestionProcessorDockerComposeRemoteBase.class.getClassLoader();
    URL         url             = testClassLoader.getResource("graphdb-conf/client.yml");

    runnerBr = TestRunners.newTestRunner(ptpcBr);
    runnerBr.setValidateExpressionUsage(true);

    runnerBr.setProperty(PROP_IS_EMBEDDED_SERVER, "false");
    runnerBr.setProperty(PROP_CONF_URI, url.toURI().toString());
    runnerBr.addControllerService("Demo_CRM_CSVReader", service, controllerSvcProps);
    ptpcBr.useEmbeddedServer = false;

    ptpcBr.queryAttribPrefixStr = "pg_";
    runnerBr.enableControllerService(service);
    runnerBr.setProperty(PROP_RECORD_READER, "Demo_CRM_CSVReader");
    Bindings bindings = new SimpleBindings();

    runBulkRequestQuery(bindings, "g.V().drop().iterate()");

  }

  public void setupTinkerpopClient() throws Exception
  {
    ptpc = new PontusTinkerPopClient();
    ptpc.enableStopDuringTestsHook = false;

    PropertyDescriptor PROP_IS_EMBEDDED_SERVER;
    PropertyDescriptor PROP_CONF_URI;

    PROP_IS_EMBEDDED_SERVER = ptpc.getPropertyDescriptor("Tinkerpop Embedded Server");
    PROP_CONF_URI = ptpc.getPropertyDescriptor("Tinkerpop Client configuration URI");

    ClassLoader testClassLoader = TestIngestionProcessorDockerComposeRemoteBase.class.getClassLoader();
    URL         url             = testClassLoader.getResource("graphdb-conf/client.yml");

    runner = TestRunners.newTestRunner(ptpc);
    runner.setValidateExpressionUsage(true);
    runner.setProperty(PROP_IS_EMBEDDED_SERVER, "false");
    runner.setProperty(PROP_CONF_URI, url.toURI().toString());

    ptpc.useEmbeddedServer = false;
    Bindings bindings = new SimpleBindings();

    ptpc.queryAttribPrefixStr = "pg_";
    runSimpleQuery(bindings, "g.V().drop().iterate()");

    // Also set up the bulk request client here... and add some data
    setupTinkerpopBulkRecordClient();
    testCSVRecordsCommon("phase1.csv");

  }

  public List<MockFlowFile> testCSVRecordsCommon(String batchFileName) throws Exception
  {

    /* Load a batch of 2 requests separated by CDP_DELIMITER into the tinkerpop nifi processor*/
    Map<String, String> attribs = new HashMap<>();
    attribs.put("pg_poleJsonStr",
        IOUtils.toString(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), StandardCharsets.UTF_8));
    attribs.put("pg_lastErrorStr", "");
    attribs.put("pg_currDate", new Date().toString());

    runnerBr.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
    runnerBr.run();

    List<MockFlowFile> result = runnerBr.getFlowFilesForRelationship(ptpcBr.REL_SUCCESS);

    /* check that we have a successful result */
    runnerBr.assertAllFlowFilesTransferred(ptpcBr.REL_SUCCESS, 1);

    String data = new String(result.get(0).toByteArray());
    assertNotNull(data);

    /* extract the PROP_QUERY results */
    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(poleRes);

    return result;

  }



  public Bindings getBulkRecordAttribs(List<MockFlowFile> result)
  {
    Bindings attribs = getBulkRequestBindings(result.get(0));
    attribs.put("pg_lastErrorStr", "");

    attribs.put("pg_currDate"
        , "Wed Feb 06 09:21:32 UTC 2019");
    attribs.put("pg_metadataController"
        , "abc inc");
    attribs.put("pg_metadataGDPRStatus"
        , "Personal");
    attribs.put("pg_metadataLineage"
        , "https://randomuser.me/api/?format=csv");
    attribs.put("pg_metadataLineageLocationTag"
        , "GB");
    attribs.put("pg_metadataLineageServerTag"
        , "GDPR-AWS-APP-SERVER");
    attribs.put("pg_metadataProcessor"
        , "cdf inc");
    attribs.put("pg_metadataRedaction"
        , "/org/dpt/project/app");
    attribs.put("pg_metadataStatus"
        , "New");
    attribs.put("pg_metadataVersion"
        , "1");
    attribs.put("pg_nlp_res_address"
        , "[]");
    attribs.put("pg_nlp_res_city"
        , "[]");
    attribs.put("pg_nlp_res_cred_card"
        , "[]");
    attribs.put("pg_nlp_res_date"
        , "[\"06/07/1993\"]");
    attribs.put("pg_nlp_res_emailaddress"
        , "[]");
    attribs.put("pg_nlp_res_location"
        , "[]");
    attribs.put("pg_nlp_res_money"
        , "[]");
    attribs.put("pg_nlp_res_organization"
        ,
        "[]");
    attribs.put("pg_nlp_res_person", "[ \"John\"]");
    attribs.put("pg_nlp_res_phone", "[]");

    attribs.put("pg_nlp_res_policy_number"
        ,
        "[\"112323443\"]");
    attribs.put("pg_nlp_res_post_code"
        ,
        "[null,\"u201CD\",\"he 14\",\"UK 08\",\"ia 08\",\"on 14\",\"u20\",\"US 87\",\"ca 08\",\"ic 29\",\"ia 18\",\"RN65\",\"es 18\",\"u201Cf\"]");
    attribs.put("pg_nlp_res_road"
        , "[]");
    attribs.put("pg_nlp_res_time"
        , "[]");
    attribs.put("pg_nlp_res_twitterhandle"
        , "[]");
    attribs.put("pg_nlp_res_url"
        , "[]");
    attribs.put("priority"
        , "0");
    attribs.put("pg_content",
        "{\"text\":\"Hi  All \\u2013 Reminder for the session \\u201CDigital Customer Acquisition in Insurance\\u201D by Sandeep Manchanda and Chayan Dasgupta on 14th November.\\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nTo:  All Band D & above, and Band C in Insurance BU\\r\\n\\r\\n \\r\\n\\r\\n\\r\\n\\r\\n                                                                                                                            \\r\\n\\r\\nHi All,\\r\\n\\r\\n \\r\\n\\r\\nDigital transformation has put the spotlight on customer experience as a key business outcome. In the insurance industry, the entire customer journey is being reimagined. And customer acquisition has been at the forefront of this transformation. \\r\\n\\r\\n \\r\\n\\r\\nI am pleased to invite you to the IntelliTalk on Digital Customer Acquisition in Insurance by Sandeep Manchanda, VP, Global Head of Digital Customer Acquisition and Chayan Dasgupta, VP Technology\\/Product Development, on the 14th November at 9 am \\u201310 am Eastern.\\r\\n\\r\\n \\r\\n\\r\\nThis session will focus on:\\r\\n\\r\\n*         What are key drivers of digital customer acquisition in Insurance\\r\\n\\r\\n*         What new innovations by InsurTechs and incumbents have entered the market\\r\\n\\r\\n*         EXL\\u2019s digital customer acquisition strategy in Insurance\\r\\n\\r\\n*         Review EXL\\u2019s Digital Customer Acquisition (DCA) platform \\r\\n\\r\\n \\r\\n\\r\\nTo prepare for the future, carriers are augmenting their \\u201Cfeet-on-the-street\\u201D customer acquisition model with a more agile, digital strategy by deploying end-to-end digital platforms. Companies have the opportunity to achieve profitable distribution by acquiring and onboarding sustainable customers more quickly and at a lower cost than traditional methods.\\r\\n\\r\\n \\r\\n\\r\\nDATE: 14th November, 9.00 am\\u201310.00 am Eastern, 7:30 pm\\u20138.30 pm IST \\r\\n\\r\\n \\r\\n\\r\\nThank you to Sandeep Manchanda and Chayan Dasgupta for sharing their insights on how EXL is applying Digital Intelligence to redefine customer acquisition for our Insurance clients. \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\nJoin Skype Meeting <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H>       \\r\\n\\r\\nTrouble Joining? Try Skype Web App <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H?sl=1>  \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nJoin by Phone\\r\\n\\r\\nFind a local number <http:\\/\\/www.intercall.com\\/l\\/dial-in-number-lookup.php>  \\r\\n\\r\\n \\r\\n\\r\\nConference ID: 9549110989 \\r\\n\\r\\n \\r\\n\\r\\nHelp <http:\\/\\/go.microsoft.com\\/fwlink\\/?LinkId=389737>    \\r\\n\\r\\n \\r\\n\\r\\nUS 8773614628\\r\\n\\r\\nIndia 180030106096\\r\\n\\r\\nPhilippines 180011101824, 180087989954\\r\\n\\r\\nUK 08003761896\\r\\n\\r\\nCzech Republic 296180005\\r\\n\\r\\nRomania 0800895570\\r\\n\\r\\nSouth Africa 0800014682 \\r\\n\\r\\n[!\\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\n \\r\\n\\r\\n\\n\",\"features\":{\"entities\":{}}}");

    return attribs;
  }

}


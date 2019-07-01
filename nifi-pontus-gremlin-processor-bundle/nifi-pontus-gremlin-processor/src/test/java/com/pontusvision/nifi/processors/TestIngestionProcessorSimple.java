package com.pontusvision.nifi.processors;

import com.jayway.jsonpath.JsonPath;
import org.apache.nifi.util.MockFlowFile;
import org.junit.Before;
import org.junit.Test;

import javax.script.Bindings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestIngestionProcessorSimple extends TestIngestionProcessorDockerComposeRemoteBase
{

  String queryStr2 = ""
      + "\n"
      + "def rulesStr = '''\n"
      + "{\n"
      + "  \"updatereq\":\n"
      + "  {\n"
      + "    \"vertices\":\n"
      + "\t[\n"
      + "\t  {\n"
      + "\t\t\"label\": \"Person.Natural\"\n"
      + "\t\t,\"percentageThreshold\": %s\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Person.Natural.Full_Name_fuzzy\"\n"
      + "\t\t   ,\"val\": \"${person}\"\n"
      + "\t\t   ,\"predicate\": \"textContainsFuzzy\"\n"
      + "\t\t   ,\"type\":\"[Ljava.lang.String;\"\n"
      + "\t\t   ,\"excludeFromUpdate\": true\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t   ,\"postProcessor\": \"${it?.toUpperCase()?.trim()}\"\n"
      + "\t\t   \n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Natural.Last_Name\"\n"
      + "\t\t   ,\"val\": \"${person}\"\n"
      + "\t\t   ,\"predicate\": \"textContainsFuzzy\"\n"
      + "\t\t   ,\"type\":\"[Ljava.lang.String;\"\n"
      + "\t\t   ,\"excludeFromUpdate\": true\n"
      + "\t\t   ,\"postProcessor\": \"${it?.toUpperCase()?.trim()}\"\n"
      + "\t\t  }\n"
      + "\t\t]\n"
      + "\t  }\n"
      + "\t ,{\n"
      + "\t\t\"label\": \"Location.Address\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Location.Address.parser.postcode\"\n"
      + "\t\t   ,\"val\": \"${postcode}\"\n"
      + "\t\t   ,\"type\":\"[Ljava.lang.String;\"\n"
      + "\t\t   ,\"excludeFromUpdate\": true\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t   ,\"postProcessorVar\": \"eachPostCode\"\n"
      + "\t\t   ,\"postProcessor\": \"${com.pontusvision.utils.PostCode.format(eachPostCode)}\"\n"
      + "\t\t  }\n"
      + "\t\t]\n"
      + "\n"
      + "\t  }\n"
      + "\t ,{\n"
      + "\t\t\"label\": \"Object.Email_Address\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Object.Email_Address.Email\"\n"
      + "\t\t   ,\"val\": \"${email}\"\n"
      + "\t\t   ,\"type\":\"[Ljava.lang.String;\"\n"
      + "\t\t   ,\"excludeFromUpdate\": true\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t]\n"
      + "\n"
      + "\t  }\n"
      + "\t ,{\n"
      + "\t\t\"label\": \"Object.Insurance_Policy\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Object.Insurance_Policy.Number\"\n"
      + "\t\t   ,\"val\": \"${policy_number}\"\n"
      + "\t\t   ,\"type\":\"[Ljava.lang.String;\"\n"
      + "\t\t   ,\"excludeFromUpdate\": true\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t]\n"
      + "\n"
      + "\t  }\n"
      + "\t ,{\n"
      + "\t\t\"label\": \"Event.Ingestion\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Type\"\n"
      + "\t\t   ,\"val\": \"Outlook PST Files\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Operation\"\n"
      + "\t\t   ,\"val\": \"Unstructured Data Insertion\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Domain_b64\"\n"
      + "\t\t   ,\"val\": \"${original_request?.bytes?.encodeBase64()?.toString()}\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Domain_Unstructured_Data_b64\"\n"
      + "\t\t   ,\"val\": \"${pg_content?.bytes?.encodeBase64()?.toString()}\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Metadata_Create_Date\"\n"
      + "\t\t   ,\"val\": \"${new Date()}\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t   \n"
      + "\t\t  }\n"
      + "\t   \n"
      + "\t\t]\n"
      + "\t  }\n"
      + "     ,{\n"
      + "\t\t\"label\": \"Event.Group_Ingestion\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Event.Group_Ingestion.Metadata_Start_Date\"\n"
      + "\t\t   ,\"val\": \"${pg_currDate}\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Group_Ingestion.Metadata_End_Date\"\n"
      + "\t\t   ,\"val\": \"${new Date()}\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t   ,\"excludeFromSubsequenceSearch\": true\n"
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t  }\n"
      + "\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Group_Ingestion.Type\"\n"
      + "\t\t   ,\"val\": \"Outlook PST Files\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Group_Ingestion.Operation\"\n"
      + "\t\t   ,\"val\": \"Unstructured Data Insertion\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t  }\n"
      + "\t   \n"
      + "\t\t]\n"
      + "\t  }\n"
      + "\n"
      + "\t]\n"
      + "   ,\"edges\":\n"
      + "    [\n"
      + "      { \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Person.Natural\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"
      + "     ,{ \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Event.Group_Ingestion\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"
      + "    ]\n"
      + "  }\n"
      + "}\n"
      + "'''\n"
      + "\n"
      + "groovy.json.JsonSlurper slurper = new groovy.json.JsonSlurper();\n"
      + "\n"
      + "\n"
      + "def bindings = [:];\n"
      + "\n"
      + "bindings['metadataController'] = \"${pg_metadataController}\";\n"
      + "bindings['metadataGDPRStatus'] = \"${pg_metadataGDPRStatus}\";\n"
      + "bindings['metadataLineage'] = \"${pg_metadataLineage}\";\n"
      + "bindings['address'] = \"${pg_nlp_res_address}\";\n"
      + "//bindings['company'] = \"${pg_nlp_res_company?:[]}\";\n"
      + "bindings['cred_card'] = \"${pg_nlp_res_cred_card}\";\n"
      + "bindings['email'] = \"${pg_nlp_res_emailaddress}\";\n"
      + "bindings['location'] = \"${pg_nlp_res_location}\";\n"
      + "bindings['pg_currDate'] = \"${pg_currDate}\";\n"
      + "\n"
      + "def parsedContent = slurper.parseText(pg_content);\n"
      + "\n"
      + "bindings['pg_content'] = parsedContent.text;\n"
      + "\n"
      + "bindings['city'] = \"${pg_nlp_res_city}\";\n"
      + "\n"
      + "\n"
      + "\n"
      + "\n"
      + "\n"
      + "def personFilter = ['Name insured person: ','1: ','Self','name: ','0','1','Name insured 1: ','Name: ','2','0: ','1: ',' 1: ']\n"
      + "// def personNamesRawList = slurper.parseText(\"${pg_nlp_res_person}\")\n"
      + "// def personNameSplitList = []\n"
      + "// personNamesRawList?.each{ personName ->\n"
      + "// \n"
      + "//   def passedFilter = personName != null && personName.length() > 2 && !( personName in personFilter);\n"
      + "// \n"
      + "//   if (passedFilter){\n"
      + "//     personNameSplitList << personName;\n"
      + "//     String[] personNameSplit = personName?.split()\n"
      + "//     personNameSplit?.each{ splitPersonName ->\n"
      + "//   \n"
      + "//       if (splitPersonName != \"\")\n"
      + "//       personNameSplitList << splitPersonName\n"
      + "//     }\n"
      + "//   }\n"
      + "// }\n"
      + "\n"
      + "\n"
      + "\n"
      + "bindings['person'] = \"${com.pontusvision.utils.NLPCleaner.filter(pg_nlp_res_person,(boolean)true,(Set<String>)personFilter) as String}\";\n"
      + "// bindings['person'] = \"${pg_nlp_res_person}\";\n"
      + "bindings['phone'] = \"${pg_nlp_res_phone}\";\n"
      + "bindings['postcode'] = \"${pg_nlp_res_post_code}\";\n"
      + "bindings['policy_number'] = \"${pg_nlp_res_policy_number}\";\n"
      + "\n"
      + "\n"
      + "\n"
      + "StringBuffer sb = new StringBuffer ()\n"
      + "\n"
      + "try{\n"
      + "  sb.append(\"\\n\\nbindings: ${bindings}\");\n"
      + "   \n"
      + "  ingestDataUsingRules(graph, g, bindings, rulesStr, sb)\n"
      + "}\n"
      + "catch (Throwable t){\n"
      + "    String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t)\n"
      + "\n"
      + "    sb.append(\"\\n$t\\n$stackTrace\")\n"
      + "\t\n"
      + "\tthrow new Throwable(sb.toString())\n"
      + "\n"
      + "\n"
      + "}\n"
      + "sb.toString()";

  @Before public void setup() throws Exception
  {
    setupTinkerpopClient();

  }

  @Test public void testNLPNoValues() throws Exception
  {

    setClientQuery(String.format(queryStr2, "45.0"));

    /* Load a batch of 2 requests separated by CDP_DELIMITER into the tinkerpop nifi processor*/
    Map<String, String> attribs = new HashMap<>();
    //    attribs.put("pg_poleJsonStr",
    //        IOUtils.toString(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), StandardCharsets.UTF_8));
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
        , "[\"\\/M2NRN65H?sl\u003d1\u003e\",\"EXL\\u2019s\"]");
    attribs.put("pg_nlp_res_emailaddress"
        , "[]");
    attribs.put("pg_nlp_res_location"
        , "[]");
    attribs.put("pg_nlp_res_money"
        , "[]");
    attribs.put("pg_nlp_res_organization"
        , "[]");
    attribs.put("pg_nlp_res_person"
        , "[]");
    attribs.put("pg_nlp_res_phone"
        , "[]");
    attribs.put("pg_nlp_res_policy_number"
        , "[]");
    attribs.put("pg_nlp_res_post_code"
        , "[]");
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
        "{\"text\":\"Hi All \\u2013 Reminder for the session \\u201CDigital Customer Acquisition in Insurance\\u201D by Sandeep Manchanda and Chayan Dasgupta on 14th November.\\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nTo:  All Band D & above, and Band C in Insurance BU\\r\\n\\r\\n \\r\\n\\r\\n\\r\\n\\r\\n                                                                                                                            \\r\\n\\r\\nHi All,\\r\\n\\r\\n \\r\\n\\r\\nDigital transformation has put the spotlight on customer experience as a key business outcome. In the insurance industry, the entire customer journey is being reimagined. And customer acquisition has been at the forefront of this transformation. \\r\\n\\r\\n \\r\\n\\r\\nI am pleased to invite you to the IntelliTalk on Digital Customer Acquisition in Insurance by Sandeep Manchanda, VP, Global Head of Digital Customer Acquisition and Chayan Dasgupta, VP Technology\\/Product Development, on the 14th November at 9 am \\u201310 am Eastern.\\r\\n\\r\\n \\r\\n\\r\\nThis session will focus on:\\r\\n\\r\\n*         What are key drivers of digital customer acquisition in Insurance\\r\\n\\r\\n*         What new innovations by InsurTechs and incumbents have entered the market\\r\\n\\r\\n*         EXL\\u2019s digital customer acquisition strategy in Insurance\\r\\n\\r\\n*         Review EXL\\u2019s Digital Customer Acquisition (DCA) platform \\r\\n\\r\\n \\r\\n\\r\\nTo prepare for the future, carriers are augmenting their \\u201Cfeet-on-the-street\\u201D customer acquisition model with a more agile, digital strategy by deploying end-to-end digital platforms. Companies have the opportunity to achieve profitable distribution by acquiring and onboarding sustainable customers more quickly and at a lower cost than traditional methods.\\r\\n\\r\\n \\r\\n\\r\\nDATE: 14th November, 9.00 am\\u201310.00 am Eastern, 7:30 pm\\u20138.30 pm IST \\r\\n\\r\\n \\r\\n\\r\\nThank you to Sandeep Manchanda and Chayan Dasgupta for sharing their insights on how EXL is applying Digital Intelligence to redefine customer acquisition for our Insurance clients. \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\nJoin Skype Meeting <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H>       \\r\\n\\r\\nTrouble Joining? Try Skype Web App <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H?sl=1>  \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nJoin by Phone\\r\\n\\r\\nFind a local number <http:\\/\\/www.intercall.com\\/l\\/dial-in-number-lookup.php>  \\r\\n\\r\\n \\r\\n\\r\\nConference ID: 9549110989 \\r\\n\\r\\n \\r\\n\\r\\nHelp <http:\\/\\/go.microsoft.com\\/fwlink\\/?LinkId=389737>    \\r\\n\\r\\n \\r\\n\\r\\nUS 8773614628\\r\\n\\r\\nIndia 180030106096\\r\\n\\r\\nPhilippines 180011101824, 180087989954\\r\\n\\r\\nUK 08003761896\\r\\n\\r\\nCzech Republic 296180005\\r\\n\\r\\nRomania 0800895570\\r\\n\\r\\nSouth Africa 0800014682 \\r\\n\\r\\n[!\\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\n \\r\\n\\r\\n\\n\",\"features\":{\"entities\":{}}}");

    runner.enqueue("  ", attribs);
    //    runnerBr.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
    runner.run();

    List<MockFlowFile> result = runner.getFlowFilesForRelationship(ptpc.REL_SUCCESS);

    /* check that we have a successful result */
    runner.assertAllFlowFilesTransferred(ptpc.REL_SUCCESS, 1);

    String data = new String(result.get(0).toByteArray());
    assertNotNull(data);

    /* extract the PROP_QUERY results */
    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(poleRes);

    Bindings bindings = getSimpleBindings(result.get(0));


    String  data2 = runSimpleQuery(bindings,
        "g.V().has('Metadata.Type.Person.Natural',eq('Person.Natural')).count()");

    Integer numItemsWithGUID = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("No people records expected", 0, (int) numItemsWithGUID);

    //    assertEquals(poleRes.split("SANDEEP").length, 169);
  }


  @Test public void testIssueNLPQueryStuck() throws Exception
  {

    setClientQuery(String.format(queryStr2, "45.0"));

    /* Load a batch of 2 requests separated by CDP_DELIMITER into the tinkerpop nifi processor*/
    Map<String, String> attribs = new HashMap<>();
    //    attribs.put("pg_poleJsonStr",
    //        IOUtils.toString(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), StandardCharsets.UTF_8));
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
        , "[\"\\/M2NRN65H?sl\u003d1\u003e\",\"EXL\\u2019s\"]");
    attribs.put("pg_nlp_res_emailaddress"
        , "[]");
    attribs.put("pg_nlp_res_location"
        , "[\"0800895570\r\n\r\nSouth Africa\"]");
    attribs.put("pg_nlp_res_money"
        , "[]");
    attribs.put("pg_nlp_res_organization"
        ,
        "[\"Digital Customer Acquisition\",\"Global Head\",\"\u201310\",\"Eastern\",\"IST\",\"VP Technology \\/Product Development\",\"Republic\"]");
    attribs.put("pg_nlp_res_person"
        ,
        "[null,\"All\",\"All \u2013 Reminder for the session \u201CDigital Customer Acquisition in Insurance\u201D by Sandeep Manchanda and Chayan Dasgupta on 14th November.\r\n\r\n \r\n\r\n \r\n\r\nTo:  All Band D \u0026 above\"]");
    attribs.put("pg_nlp_res_phone"
        ,
        "[null,\"77\",\"8995\",\"29\",\"1110\",\"1098\",\"491\",\"361\",\"4628\",\"1\",\"376\",\"1896\",\"0005\",\"301\",\"0146\",\"8\",\"800\",\"0609\",\"879\",\"8955\",\"95\",\"618\"]");
    attribs.put("pg_nlp_res_policy_number"
        ,
        "[\"87736146\",\"08000146\",\"08008955\",\"18003010\",\"18008798\",\"95491109\",\"29618000\",\"18001110\",\"08003761\"]");
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
        "{\"text\":\"Hi All \\u2013 Reminder for the session \\u201CDigital Customer Acquisition in Insurance\\u201D by Sandeep Manchanda and Chayan Dasgupta on 14th November.\\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nTo:  All Band D & above, and Band C in Insurance BU\\r\\n\\r\\n \\r\\n\\r\\n\\r\\n\\r\\n                                                                                                                            \\r\\n\\r\\nHi All,\\r\\n\\r\\n \\r\\n\\r\\nDigital transformation has put the spotlight on customer experience as a key business outcome. In the insurance industry, the entire customer journey is being reimagined. And customer acquisition has been at the forefront of this transformation. \\r\\n\\r\\n \\r\\n\\r\\nI am pleased to invite you to the IntelliTalk on Digital Customer Acquisition in Insurance by Sandeep Manchanda, VP, Global Head of Digital Customer Acquisition and Chayan Dasgupta, VP Technology\\/Product Development, on the 14th November at 9 am \\u201310 am Eastern.\\r\\n\\r\\n \\r\\n\\r\\nThis session will focus on:\\r\\n\\r\\n*         What are key drivers of digital customer acquisition in Insurance\\r\\n\\r\\n*         What new innovations by InsurTechs and incumbents have entered the market\\r\\n\\r\\n*         EXL\\u2019s digital customer acquisition strategy in Insurance\\r\\n\\r\\n*         Review EXL\\u2019s Digital Customer Acquisition (DCA) platform \\r\\n\\r\\n \\r\\n\\r\\nTo prepare for the future, carriers are augmenting their \\u201Cfeet-on-the-street\\u201D customer acquisition model with a more agile, digital strategy by deploying end-to-end digital platforms. Companies have the opportunity to achieve profitable distribution by acquiring and onboarding sustainable customers more quickly and at a lower cost than traditional methods.\\r\\n\\r\\n \\r\\n\\r\\nDATE: 14th November, 9.00 am\\u201310.00 am Eastern, 7:30 pm\\u20138.30 pm IST \\r\\n\\r\\n \\r\\n\\r\\nThank you to Sandeep Manchanda and Chayan Dasgupta for sharing their insights on how EXL is applying Digital Intelligence to redefine customer acquisition for our Insurance clients. \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\nJoin Skype Meeting <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H>       \\r\\n\\r\\nTrouble Joining? Try Skype Web App <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H?sl=1>  \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nJoin by Phone\\r\\n\\r\\nFind a local number <http:\\/\\/www.intercall.com\\/l\\/dial-in-number-lookup.php>  \\r\\n\\r\\n \\r\\n\\r\\nConference ID: 9549110989 \\r\\n\\r\\n \\r\\n\\r\\nHelp <http:\\/\\/go.microsoft.com\\/fwlink\\/?LinkId=389737>    \\r\\n\\r\\n \\r\\n\\r\\nUS 8773614628\\r\\n\\r\\nIndia 180030106096\\r\\n\\r\\nPhilippines 180011101824, 180087989954\\r\\n\\r\\nUK 08003761896\\r\\n\\r\\nCzech Republic 296180005\\r\\n\\r\\nRomania 0800895570\\r\\n\\r\\nSouth Africa 0800014682 \\r\\n\\r\\n[!\\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\n \\r\\n\\r\\n\\n\",\"features\":{\"entities\":{}}}");

    runner.enqueue("  ", attribs);
    //    runnerBr.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
    runner.run();

    List<MockFlowFile> result = runner.getFlowFilesForRelationship(ptpc.REL_SUCCESS);

    /* check that we have a successful result */
    runner.assertAllFlowFilesTransferred(ptpc.REL_SUCCESS, 1);

    String data = new String(result.get(0).toByteArray());
    assertNotNull(data);

    /* extract the PROP_QUERY results */
    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(poleRes);

    assertEquals(9, poleRes.split("SANDEEP").length);
  }

}


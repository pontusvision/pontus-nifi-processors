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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestIngestionProcessor
{

  private static final String TEST_DATA_RESOURCE_DIR = "csv-data/";

  protected static File       DEFAULT_INSTALL_DIR = new File(System.getProperty("java.io.tmpdir"), "data-dir");
  protected        TestRunner runner;
  protected        TestRunner runnerBr;
  PontusTinkerPopClient           ptpc;
  PontusTinkerPopClientRecordBulk ptpcBr;
  PropertyDescriptor              embeddedServer;
  PropertyDescriptor              confURI;
  PropertyDescriptor              RECORD_READER;
  PropertyDescriptor              query;
  protected String queryStr =
      "\n" + "\n"
          + "def rulesStr = '''\n"
          + "\n"
          + "{\n"
          + "  \"updatereq\":\n"
          + "  {\n"
          + "\n"
          + "    \"vertices\":\n"
          + "\t[\n"
          + "\t  {\n"
          + "\t\t\"label\": \"Person\"\n"
          + "\t   ,\"props\":\n"
          + "\t\t[\n"
          + "\t\t  {\n"
          + "\t\t\t\"name\": \"Person.Full_Name\"\n"
          + "\t\t   ,\"val\": \"${pg_First_Name?.toUpperCase()?.trim()} ${pg_Last_Name?.toUpperCase()?.trim()}\"\n"
          + "\t\t   ,\"predicate\": \"eq\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Person.Full_Name_fuzzy\"\n"
          + "\t\t   ,\"val\": \"${pg_First_Name?.toUpperCase()?.trim()} ${pg_Last_Name?.toUpperCase()?.trim()}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Person.Last_Name\"\n"
          + "\t\t   ,\"val\": \"${pg_Last_Name?.toUpperCase()?.trim()}\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Person.Date_Of_Birth\"\n"
          + "\t\t   ,\"val\": \"${pg_DateofBirth}\"\n"
          + "\t\t   ,\"type\": \"java.util.Date\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Person.Gender\"\n"
          + "\t\t   ,\"val\": \"${pg_Sex.toUpperCase()}\"\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Person.Customer_ID\"\n"
          + "\t\t   ,\"val\": \"${pg_Customer_ID}\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Person.Title\"\n"
          + "\t\t   ,\"val\": \"${'MALE' == pg_Sex.toUpperCase()? 'MR':'MS'}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Person.Nationality\"\n"
          + "\t\t   ,\"val\": \"GB\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\n"
          + "\t\t]\n"
          + "\t  }\n"
          + "\t ,{\n"
          + "\t\t\"label\": \"Location.Address\"\n"
          + "\t   ,\"props\":\n"
          + "\t    [\n" + "\t      {\n"
          + "\t    \t\"name\": \"Location.Address.Full_Address\"\n" + "\t       ,\"val\": \"${pg_Address}\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\n" + "\t      }\n"
          + "\t     ,{\n"
          + "\t    \t\"name\": \"Location.Address.parser.postcode\"\n"
          + "\t       ,\"val\": \"${com.pontusvision.utils.PostCode.format(pg_Post_Code)}\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\n"
          + "\t      }\n"
          + "\t     ,{\n"
          + "\t    \t\"name\": \"Location.Address.parser\"\n"
          + "\t       ,\"val\": \"${pg_Address}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t   ,\"type\": \"com.pontusvision.utils.LocationAddress\"\n"
          + "\n"
          + "\t      }\n"
          + "\t     ,{\n"
          + "\t    \t\t\"name\": \"Location.Address.Post_Code\"\n"
          + "\t       ,\"val\": \"${com.pontusvision.utils.PostCode.format(pg_Post_Code)}\"\n"
          + "\t       ,\"excludeFromSearch\": true\n"
          + "\t      }\n"
          + "\t    ]\n"
          + "\n"
          + "\t  }\n"
          + "\t ,{\n"
          + "\t\t\"label\": \"Object.Email_Address\"\n"
          + "\t\t,\"props\":\n"
          + "\t\t[\n"
          + "\t\t  {\n"
          + "\t\t\t\"name\": \"Object.Email_Address.Email\"\n"
          + "\t\t   ,\"val\": \"${pg_Email_address}\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\n"
          + "\t\t  }\n"
          + "\t\t]\n"
          + "\n"
          + "\t  }\n"
          + "\t ,{\n"
          + "\t\t\"label\": \"Object.Insurance_Policy\"\n"
          + "\t\t,\"props\":\n"
          + "\t\t[\n"
          + "\t\t  {\n"
          + "\t\t\t\"name\": \"Object.Insurance_Policy.Number\"\n"
          + "\t\t   ,\"val\": \"${pg_Policynumber}\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Object.Insurance_Policy.Type\"\n"
          + "\t\t   ,\"val\": \"${pg_PolicyType}\"\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\t\"name\": \"Object.Insurance_Policy.Status\"\n"
          + "\t\t   ,\"val\": \"${pg_PolicyStatus}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t    \"name\": \"Object.Insurance_Policy.Renewal_Date\"\n"
          + "\t\t   ,\"val\": \"${pg_RenewalDate}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t   ,\"type\": \"java.util.Date\"\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t    \"name\": \"Object.Insurance_Policy.Product_Type\"\n"
          + "\t\t   ,\"val\": \"${pg_TypeOfinsurance}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t]\n"
          + "\n"
          + "\t  }\n"
          + "\t ,{\n"
          + "\t\t\"label\": \"Event.Ingestion.Group\"\n"
          + "\t   ,\"props\":\n"
          + "\t\t[\n"
          + "\t\t  {\n"
          + "\t\t\t\"name\": \"Event.Ingestion.Group.Metadata_Start_Date\"\n"
          + "\t\t   ,\"val\": \"${pg_currDate}\"\n"
          + "\t\t   ,\"mandatoryInSearch\": true\n"
          + "\t\t   ,\"excludeFromSearch\": false\n"
          + "\t\t   ,\"type\": \"java.util.Date\"\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Event.Ingestion.Group.Metadata_End_Date\"\n"
          + "\t\t   ,\"val\": \"${new Date()}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t   ,\"type\": \"java.util.Date\"\n"
          + "\t\t  }\n"
          + "\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Event.Ingestion.Group.Type\"\n"
          + "\t\t   ,\"val\": \"CRM System CSV File\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Event.Ingestion.Group.Operation\"\n"
          + "\t\t   ,\"val\": \"Structured Data Insertion\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t   \n"
          + "\t\t]\n"
          + "\t  }\n"
          + "\t ,{\n"
          + "\t\t\"label\": \"Event.Ingestion\"\n"
          + "\t   ,\"props\":\n"
          + "\t\t[\n"
          + "\t\t  {\n"
          + "\t\t\t\"name\": \"Event.Ingestion.Type\"\n"
          + "\t\t   ,\"val\": \"CRM System CSV File\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Event.Ingestion.Operation\"\n"
          + "\t\t   ,\"val\": \"Structured Data Insertion\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Event.Ingestion.Domain_b64\"\n"
          + "\t\t   ,\"val\": \"${original_request?.bytes?.encodeBase64()?.toString()}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Event.Ingestion.Metadata_Create_Date\"\n"
          + "\t\t   ,\"val\": \"${new Date()}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t   ,\"type\": \"java.util.Date\"\n"
          + "\t\t  }\n"
          + "\t   \n"
          + "\t\t]\n"
          + "\t  }\n"
          + "\n"
          + "\t  ,{\n"
          + "\t\t\"label\": \"Event.Consent\"\n"
          + "\t   ,\"props\":\n"
          + "\t\t[\n"
          + "\t\t  {\n"
          + "\t\t\t\"name\": \"Event.Consent.Status\"\n"
          + "\t\t   ,\"val\": \"${pg_Permisssion_to_Contact?'Consent': 'No Consent'}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t  }\n"
          + "\t\t ,{\n"
          + "\t\t\t\"name\": \"Event.Consent.Date\"\n"
          + "\t\t   ,\"val\": \"${new Date()}\"\n"
          + "\t\t   ,\"excludeFromSearch\": true\n"
          + "\t\t   ,\"type\": \"java.util.Date\"\n"
          + "\n"
          + "\t\t  }\n"
          + "\t   \n"
          + "\t\t]\n"
          + "\t  }\n"
          + "\t   ,{\n"
          + "\t\t\"label\": \"Object.Privacy_Notice\"\n"
          + "\t   ,\"props\":\n"
          + "\t\t[\n"
          + "\t\t  {\n"
          + "\t\t\t\"name\": \"Object.Privacy_Notice.Who_Is_Collecting\"\n"
          + "\t\t   ,\"val\": \"[CRM System]\"\n"
          + "\t\t   ,\"excludeFromUpdate\": true\n"
          + "\t\t  }\t   \n"
          + "\t\t]\n"
          + "\t  } \n"
          + "\n"
          + "\t]\n"
          + "   ,\"edges\":\n"
          + "    [\n"

          + "      { \"label\": \"Uses_Email\", \"fromVertexLabel\": \"Person\", \"toVertexLabel\": \"Object.Email_Address\" }\n"

          + "     ,{ \"label\": \"Lives\", \"fromVertexLabel\": \"Person\", \"toVertexLabel\": \"Location.Address\"  }\n"

          + "     ,{ \"label\": \"Has_Policy\", \"fromVertexLabel\": \"Person\", \"toVertexLabel\": \"Object.Insurance_Policy\"  }\n"

          + "     ,{ \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Person\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"

          + "     ,{ \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Event.Ingestion.Group\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"

          + "     ,{ \"label\": \"Consent\", \"fromVertexLabel\": \"Person\", \"toVertexLabel\": \"Event.Consent\"  }\n"

          + "     ,{ \"label\": \"Has_Privacy_Notice\", \"fromVertexLabel\": \"Event.Consent\", \"toVertexLabel\": \"Object.Privacy_Notice\"  }\n"

          + "\t \n"
          + "    ]\n"
          + "  }\n"
          + "}\n"
          + "'''\n"
          + "StringBuffer sb = new StringBuffer ()\n"
          + "\n"

          + "try{\n"
          + "    ingestRecordListUsingRules(graph, g, listOfMaps, rulesStr, sb)\n"
          + "}\n"

          + "catch (Throwable t){\n"

          + "    String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t)\n"
          + "\n"

          + "    sb.append(\"\\n$t\\n$stackTrace\")\n"
          + "    throw new Throwable (sb.toString())\n"
          + "\n"
          + "\n"

          + "}\n"
          + "sb.toString()\n"
          + "\n";
  String queryStr2 = ""
      + "\n"
      + "def rulesStr = '''\n"
      + "{\n"
      + "  \"updatereq\":\n"
      + "  {\n"
      + "    \"vertices\":\n"
      + "\t[\n"
      + "\t  {\n"
      + "\t\t\"label\": \"Person\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Person.Full_Name_fuzzy\"\n"
      + "\t\t   ,\"val\": \"${person}\"\n"
      + "\t\t   ,\"predicate\": \"textContainsFuzzy\"\n"
      + "\t\t   ,\"type\":\"[Ljava.lang.String;\"\n"
      + "\t\t   ,\"excludeFromUpdate\": true\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t   ,\"postProcessor\": \"${it?.toUpperCase()?.trim()}\"\n"
      + "\t\t   \n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Last_Name\"\n"
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
      + "\t\t\"label\": \"Event.Ingestion.Group\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Group.Metadata_Start_Date\"\n"
      + "\t\t   ,\"val\": \"${pg_currDate}\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Group.Metadata_End_Date\"\n"
      + "\t\t   ,\"val\": \"${new Date()}\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t   ,\"excludeFromSubsequenceSearch\": true\n"
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t  }\n"
      + "\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Group.Type\"\n"
      + "\t\t   ,\"val\": \"Outlook PST Files\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Ingestion.Group.Operation\"\n"
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
      + "      { \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Person\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"
      + "     ,{ \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Event.Ingestion.Group\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"
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

    //    String osName = System.getProperty("os.name").toLowerCase();
    //    if (osName.startsWith("win"))
    //    {
    //      System.setProperty("os.arch", "x86");
    //
    //    }

    if (!DEFAULT_INSTALL_DIR.exists())
    {
      FileUtils.forceMkdir(DEFAULT_INSTALL_DIR);

      File         outFile     = File.createTempFile("datadir", ".tar.gz", DEFAULT_INSTALL_DIR);
      OutputStream out         = new FileOutputStream(outFile);
      final int    BUFFER_SIZE = 256 * 1024;
      byte[]       buf         = new byte[BUFFER_SIZE];

      File       folder      = new File(".." + File.separator + ".." + File.separator + "pontus-gdpr-graph");
      FileFilter filter      = new PrefixFileFilter("datadir.tar.gz-");
      File[]     listOfFiles = folder.listFiles(filter);

      for (File file : listOfFiles)
      {
        InputStream in = new FileInputStream(file);
        int         b  = 0;
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
            File    f       = new File(DEFAULT_INSTALL_DIR, entry.getName());
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
                                                           .withSetting(PopularProperties.CLUSTER_NAME, "my_cluster")
                                                           .withStartTimeout(2, MINUTES)

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
    FileOutputStream    fos = new FileOutputStream(fileName);
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
    URL         url             = testClassLoader.getResource("graphdb-conf/gremlin-mem.yml");

    runnerBr = TestRunners.newTestRunner(ptpcBr);
    runnerBr.setValidateExpressionUsage(true);
    runnerBr.setProperty(embeddedServer, "true");
    runnerBr.setProperty(confURI, url.toURI().toString());
    runnerBr.setProperty(query, queryStr);
    runnerBr.addControllerService("Demo_CRM_CSVReader", service, controllerSvcProps);

    runnerBr.enableControllerService(service);
    runnerBr.setProperty(RECORD_READER, "Demo_CRM_CSVReader");

    //    ptpcBr.onPropertyModified(embeddedServer, "true", "true");
    //    ptpcBr.onPropertyModified(confURI, "", url.toURI().toString());
    //    ptpcBr.onPropertyModified(query, "true", queryStr);
    //    ptpcBr.onPropertyModified(RECORD_READER, "", "Demo_CRM_CSVReader");

    runner = TestRunners.newTestRunner(ptpc);
    runner.setValidateExpressionUsage(true);
    runner.setProperty(embeddedServer, "true");
    runner.setProperty(confURI, url.toURI().toString());
    runner.setProperty(query, queryStr);

    //    ptpc.onPropertyModified(embeddedServer, "true", "true");
    //    ptpc.onPropertyModified(confURI, "", url.toURI().toString());
    //    ptpc.onPropertyModified(query, "true", queryStr);

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
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");

    Bindings bindings = ptpcBr.getBindings(result.get(0));

    byte[] res = ptpcBr.runQuery(bindings,
        "g.V().has('Object.Insurance_Policy.Number', eq('10333275')).count()");
    String  data2            = new String(res);
    Integer numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with Policy Number", 1, (int) numItemsWithGUID);

  }

  @Test public void testMatching() throws Exception
  {
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");



    Bindings attribs = ptpcBr.getBindings(result.get(0));

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
        , "[]");
    attribs.put("pg_nlp_res_emailaddress"
        , "[]");
    attribs.put("pg_nlp_res_location"
        , "[]");
    attribs.put("pg_nlp_res_money"
        , "[]");
    attribs.put("pg_nlp_res_organization"
        ,
        "[]");
    attribs.put("pg_nlp_res_person","[\"John Smith\",\"John Dailey\"]");
    attribs.put("pg_nlp_res_phone","[]");

    attribs.put("pg_nlp_res_policy_number"
        ,
        "[]");
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
    attribs.put ("pg_content", "{\"text\":\"Hi All \\u2013 Reminder for the session \\u201CDigital Customer Acquisition in Insurance\\u201D by Sandeep Manchanda and Chayan Dasgupta on 14th November.\\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nTo:  All Band D & above, and Band C in Insurance BU\\r\\n\\r\\n \\r\\n\\r\\n\\r\\n\\r\\n                                                                                                                            \\r\\n\\r\\nHi All,\\r\\n\\r\\n \\r\\n\\r\\nDigital transformation has put the spotlight on customer experience as a key business outcome. In the insurance industry, the entire customer journey is being reimagined. And customer acquisition has been at the forefront of this transformation. \\r\\n\\r\\n \\r\\n\\r\\nI am pleased to invite you to the IntelliTalk on Digital Customer Acquisition in Insurance by Sandeep Manchanda, VP, Global Head of Digital Customer Acquisition and Chayan Dasgupta, VP Technology\\/Product Development, on the 14th November at 9 am \\u201310 am Eastern.\\r\\n\\r\\n \\r\\n\\r\\nThis session will focus on:\\r\\n\\r\\n*         What are key drivers of digital customer acquisition in Insurance\\r\\n\\r\\n*         What new innovations by InsurTechs and incumbents have entered the market\\r\\n\\r\\n*         EXL\\u2019s digital customer acquisition strategy in Insurance\\r\\n\\r\\n*         Review EXL\\u2019s Digital Customer Acquisition (DCA) platform \\r\\n\\r\\n \\r\\n\\r\\nTo prepare for the future, carriers are augmenting their \\u201Cfeet-on-the-street\\u201D customer acquisition model with a more agile, digital strategy by deploying end-to-end digital platforms. Companies have the opportunity to achieve profitable distribution by acquiring and onboarding sustainable customers more quickly and at a lower cost than traditional methods.\\r\\n\\r\\n \\r\\n\\r\\nDATE: 14th November, 9.00 am\\u201310.00 am Eastern, 7:30 pm\\u20138.30 pm IST \\r\\n\\r\\n \\r\\n\\r\\nThank you to Sandeep Manchanda and Chayan Dasgupta for sharing their insights on how EXL is applying Digital Intelligence to redefine customer acquisition for our Insurance clients. \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\nJoin Skype Meeting <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H>       \\r\\n\\r\\nTrouble Joining? Try Skype Web App <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H?sl=1>  \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nJoin by Phone\\r\\n\\r\\nFind a local number <http:\\/\\/www.intercall.com\\/l\\/dial-in-number-lookup.php>  \\r\\n\\r\\n \\r\\n\\r\\nConference ID: 9549110989 \\r\\n\\r\\n \\r\\n\\r\\nHelp <http:\\/\\/go.microsoft.com\\/fwlink\\/?LinkId=389737>    \\r\\n\\r\\n \\r\\n\\r\\nUS 8773614628\\r\\n\\r\\nIndia 180030106096\\r\\n\\r\\nPhilippines 180011101824, 180087989954\\r\\n\\r\\nUK 08003761896\\r\\n\\r\\nCzech Republic 296180005\\r\\n\\r\\nRomania 0800895570\\r\\n\\r\\nSouth Africa 0800014682 \\r\\n\\r\\n[!\\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\n \\r\\n\\r\\n\\n\",\"features\":{\"entities\":{}}}");

    byte[] res = ptpcBr.runQuery(attribs,
        queryStr2);
    String  data2            = new String(res);
    Integer numItemsWithGUID = JsonPath.read(data2, "$.result.data['@value'][0]['@value']");
    assertEquals("Only one item with Policy Number", 1, (int) numItemsWithGUID);

  }



  @Test public void testIssueNLPQueryStuck() throws Exception
  {

    runner.setProperty(query, queryStr2);

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
    attribs.put ("pg_content", "{\"text\":\"Hi All \\u2013 Reminder for the session \\u201CDigital Customer Acquisition in Insurance\\u201D by Sandeep Manchanda and Chayan Dasgupta on 14th November.\\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nTo:  All Band D & above, and Band C in Insurance BU\\r\\n\\r\\n \\r\\n\\r\\n\\r\\n\\r\\n                                                                                                                            \\r\\n\\r\\nHi All,\\r\\n\\r\\n \\r\\n\\r\\nDigital transformation has put the spotlight on customer experience as a key business outcome. In the insurance industry, the entire customer journey is being reimagined. And customer acquisition has been at the forefront of this transformation. \\r\\n\\r\\n \\r\\n\\r\\nI am pleased to invite you to the IntelliTalk on Digital Customer Acquisition in Insurance by Sandeep Manchanda, VP, Global Head of Digital Customer Acquisition and Chayan Dasgupta, VP Technology\\/Product Development, on the 14th November at 9 am \\u201310 am Eastern.\\r\\n\\r\\n \\r\\n\\r\\nThis session will focus on:\\r\\n\\r\\n*         What are key drivers of digital customer acquisition in Insurance\\r\\n\\r\\n*         What new innovations by InsurTechs and incumbents have entered the market\\r\\n\\r\\n*         EXL\\u2019s digital customer acquisition strategy in Insurance\\r\\n\\r\\n*         Review EXL\\u2019s Digital Customer Acquisition (DCA) platform \\r\\n\\r\\n \\r\\n\\r\\nTo prepare for the future, carriers are augmenting their \\u201Cfeet-on-the-street\\u201D customer acquisition model with a more agile, digital strategy by deploying end-to-end digital platforms. Companies have the opportunity to achieve profitable distribution by acquiring and onboarding sustainable customers more quickly and at a lower cost than traditional methods.\\r\\n\\r\\n \\r\\n\\r\\nDATE: 14th November, 9.00 am\\u201310.00 am Eastern, 7:30 pm\\u20138.30 pm IST \\r\\n\\r\\n \\r\\n\\r\\nThank you to Sandeep Manchanda and Chayan Dasgupta for sharing their insights on how EXL is applying Digital Intelligence to redefine customer acquisition for our Insurance clients. \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\nJoin Skype Meeting <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H>       \\r\\n\\r\\nTrouble Joining? Try Skype Web App <https:\\/\\/meet.lync.com\\/exlservice\\/amit.choudhary\\/M2NRN65H?sl=1>  \\r\\n\\r\\n \\r\\n\\r\\n \\r\\n\\r\\nJoin by Phone\\r\\n\\r\\nFind a local number <http:\\/\\/www.intercall.com\\/l\\/dial-in-number-lookup.php>  \\r\\n\\r\\n \\r\\n\\r\\nConference ID: 9549110989 \\r\\n\\r\\n \\r\\n\\r\\nHelp <http:\\/\\/go.microsoft.com\\/fwlink\\/?LinkId=389737>    \\r\\n\\r\\n \\r\\n\\r\\nUS 8773614628\\r\\n\\r\\nIndia 180030106096\\r\\n\\r\\nPhilippines 180011101824, 180087989954\\r\\n\\r\\nUK 08003761896\\r\\n\\r\\nCzech Republic 296180005\\r\\n\\r\\nRomania 0800895570\\r\\n\\r\\nSouth Africa 0800014682 \\r\\n\\r\\n[!\\r\\n\\r\\n.........................................................................................................................................\\r\\n\\r\\n \\r\\n\\r\\n\\n\",\"features\":{\"entities\":{}}}");

    runner.enqueue("  ",attribs);
//    runnerBr.enqueue(TestUtils.getFileInputStream(TEST_DATA_RESOURCE_DIR + batchFileName), attribs);
    runner.run();

    List<MockFlowFile> result = runner.getFlowFilesForRelationship(ptpc.REL_SUCCESS);

    /* check that we have a successful result */
    runner.assertAllFlowFilesTransferred(ptpc.REL_SUCCESS, 1);

    String data = new String(result.get(0).toByteArray());
    assertNotNull(data);

    /* extract the query results */
    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(poleRes);

    assertEquals(poleRes.split("SANDEEP").length, 169);
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
    attribs.put("pg_currDate", new Date().toString());

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
    String  data2            = new String(res);
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

    List<MockFlowFile> result = runnerBr.getFlowFilesForRelationship(ptpc.REL_SUCCESS);

    /* check that we have a successful result */
    runnerBr.assertAllFlowFilesTransferred(ptpcBr.REL_SUCCESS, 1);

    String data = new String(result.get(0).toByteArray());
    assertNotNull(data);

    /* extract the query results */
    String poleRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(poleRes);

    return result;

    //    Integer numEntries = JsonPath.read(poleRes, "$.length()");

    //    assertEquals("Batch count preserved", 2, (int) numEntries);

    //    Integer numAssocFirstBatch  = JsonPath.read(poleRes, "$.[0].numberOfAssociationsCreated");
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
    String  data2            = new String(res);
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


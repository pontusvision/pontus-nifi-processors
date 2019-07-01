package com.pontusvision.nifi.processors;

import com.jayway.jsonpath.JsonPath;
import org.apache.nifi.util.MockFlowFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.script.Bindings;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestIngestionProcessorBulkRequests extends TestIngestionProcessorDockerComposeRemoteBase
{
  String phase1QueryStr = "\n"
      + "\n"
      + "def rulesStr = '''\n"
      + "\n"
      + "{\n"
      + "  \"updatereq\":\n"
      + "  {\n"
      + "\n"
      + "    \"vertices\":\n"
      + "\t[\n"
      + "\t  {\n"
      + "\t\t\"label\": \"Person.Natural\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Person.Natural.Full_Name\"\n"
      + "\t\t   ,\"val\": \"${pg_First_Name?.toUpperCase()?.trim()} ${pg_Last_Name?.toUpperCase()?.trim()}\"\n"
      + "\t\t   ,\"predicate\": \"eq\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Natural.Full_Name_fuzzy\"\n"
      + "\t\t   ,\"val\": \"${pg_First_Name?.toUpperCase()?.trim()} ${pg_Last_Name?.toUpperCase()?.trim()}\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Natural.Last_Name\"\n"
      + "\t\t   ,\"val\": \"${pg_Last_Name?.toUpperCase()?.trim()}\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Natural.Date_Of_Birth\"\n"
      + "\t\t   ,\"val\": \"${pg_DateofBirth}\"\n"
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Natural.Gender\"\n"
      + "\t\t   ,\"val\": \"${pg_Sex.toUpperCase()}\"\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Natural.Customer_ID\"\n"
      + "\t\t   ,\"val\": \"${pg_Customer_ID}\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Natural.Title\"\n"
      + "\t\t   ,\"val\": \"${'MALE' == pg_Sex.toUpperCase()? 'MR':'MS'}\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Person.Natural.Nationality\"\n"
      + "\t\t   ,\"val\": \"GB\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t  }\n"
      + "\n"
      + "\t\t]\n"
      + "\t  }\n"
      + "\t ,{\n"
      + "\t\t\"label\": \"Location.Address\"\n"
      + "\t   ,\"props\":\n"
      + "\t    [\n"
      + "\t      {\n"
      + "\t    \t\"name\": \"Location.Address.Full_Address\"\n"
      + "\t       ,\"val\": \"${pg_Address}\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\n"
      + "\t      }\n"
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
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t  }\n"
      + "\t\t]\n"
      + "\n"
      + "\t  }\n"
      + "\t ,{\n"
      + "\t\t\"label\": \"Event.Group_Ingestion\"\n"
      + "\t   ,\"props\":\n"
      + "\t\t[\n"
      + "\t\t  {\n"
      + "\t\t\t\"name\": \"Event.Group_Ingestion.Metadata_Start_Date\"\n"
      + "\t\t   ,\"val\": \"${pg_currDate}\"\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t   ,\"excludeFromSearch\": false\n"
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Group_Ingestion.Metadata_End_Date\"\n"
      + "\t\t   ,\"val\": \"${new Date()}\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t   ,\"type\": \"java.util.Date\"\n"
      + "\t\t  }\n"
      + "\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Group_Ingestion.Type\"\n"
      + "\t\t   ,\"val\": \"CRM System CSV File\"\n"
      + "\t\t   ,\"excludeFromSearch\": true\n"
      + "\t\t  }\n"
      + "\t\t ,{\n"
      + "\t\t\t\"name\": \"Event.Group_Ingestion.Operation\"\n"
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
      + "      { \"label\": \"Uses_Email\", \"fromVertexLabel\": \"Person.Natural\", \"toVertexLabel\": \"Object.Email_Address\" }\n"
      + "     ,{ \"label\": \"Lives\", \"fromVertexLabel\": \"Person.Natural\", \"toVertexLabel\": \"Location.Address\"  }\n"
      + "     ,{ \"label\": \"Has_Policy\", \"fromVertexLabel\": \"Person.Natural\", \"toVertexLabel\": \"Object.Insurance_Policy\"  }\n"
      + "\t ,{ \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Location.Address\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"
      + "     ,{ \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Person.Natural\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"
      + "     ,{ \"label\": \"Has_Ingestion_Event\", \"fromVertexLabel\": \"Event.Group_Ingestion\", \"toVertexLabel\": \"Event.Ingestion\"  }\n"
      + "     ,{ \"label\": \"Consent\", \"fromVertexLabel\": \"Person.Natural\", \"toVertexLabel\": \"Event.Consent\"  }\n"
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

  String queryStr3 = ""
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
      + "\t\t   ,\"predicate\": \"idxRaw:Person.Natural.MixedIdx\"\n"
      + "\t\t   ,\"type\":\"[Ljava.lang.String;\"\n"
      + "\t\t   ,\"excludeFromUpdate\": true\n"
      + "\t\t   ,\"mandatoryInSearch\": true\n"
      + "\t\t   ,\"postProcessor\": \"v.'Person.Natural.Full_Name_fuzzy':${it?.trim()}~\"\n"
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
    setupTinkerpopBulkRecordClient();
    setBulkRecordClientQuery(phase1QueryStr);
  }


  @Test public void testBatchNormalOrder() throws Exception
  {
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");

    Bindings bindings = getBulkRequestBindings(result.get(0));

    String  data2 = runBulkRequestQuery(bindings,
        "g.V().has('Object.Insurance_Policy.Number', eq('10333275')).count()");

    Integer numItemsWithGUID = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("Only one item with Policy Number", 1, (int) numItemsWithGUID);

  }


  @Test public void testMatchingScoresRawIdx90Pcnt() throws Exception
  {
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");

    // This should fall short of the 90% threshold, so no edges should be created.
    Bindings attribs = getBulkRecordAttribs(result);
    attribs.put("pg_nlp_res_person", "[\"John\"]");

    String data = runBulkRequestQuery(attribs,
        String.format(queryStr3, "90.0"));

    String actualRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(actualRes);

    data = runBulkRequestQuery(attribs,
        "double maxScoreForRawIdx = 0;\n"
            + "int maxHitsPerType = 1000;\n"
            + "  Map<Long, Double> idxQueryRes = new HashMap<>();\n"
            + "\n"
            + "  graph.indexQuery(\"Person.Natural.MixedIdx\", 'v.\"Person.Natural.Full_Name_fuzzy\":john~')?.limit(maxHitsPerType)?.vertexStream()?.forEach { org.janusgraph.core.JanusGraphIndexQuery.Result<org.janusgraph.core.JanusGraphVertex> result ->\n"
            + "    double score = result.score\n"
            + "    idxQueryRes.put((Long)result.element.id(), score);\n"
            + "    maxScoreForRawIdx = Math.max(maxScoreForRawIdx, score);\n"
            + "  }\n"
            + "  \n"
            + "  idxQueryRes\n");

    assertNotNull("Data not NULL expected", data);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Metadata.Type.Person.Natural',eq('Person.Natural')).count()");
    Integer numItemsWithGUID = Integer.parseInt(JsonPath.read(data, "$.result.data['@value'][0]"));
    assertEquals("10 people records expected", 10, (int) numItemsWithGUID);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE()");
    //    actualRes = JsonPath.read(data, "$.result.data['@value'][0]['@value']");
    assertNotNull("Data not NULL expected", data);

    String  data2 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE().count()");
    Integer numEdges = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 90%", 5, (int) numEdges);

    data2 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN DAILEY')).bothE().count()");
    numEdges = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 90%", 5, (int) numEdges);

  }

  @Test public void testMatchingScoresRawIdx45Pcnt() throws Exception
  {
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");

    // This should fall short of the 90% threshold, so no edges should be created.
    Bindings attribs = getBulkRecordAttribs(result);
    attribs.put("pg_nlp_res_person", "[\"John\"]");

    String data = runBulkRequestQuery(attribs,
        String.format(queryStr3, "45.0"));
    String actualRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(actualRes);

    data = runBulkRequestQuery(attribs,
        "double maxScoreForRawIdx = 0;\n"
            + "int maxHitsPerType = 1000;\n"
            + "  Map<Long, Double> idxQueryRes = new HashMap<>();\n"
            + "\n"
            + "  graph.indexQuery(\"Person.Natural.MixedIdx\", 'v.\"Person.Natural.Full_Name_fuzzy\":john~')?.limit(maxHitsPerType)?.vertexStream()?.forEach { org.janusgraph.core.JanusGraphIndexQuery.Result<org.janusgraph.core.JanusGraphVertex> result ->\n"
            + "    double score = result.score\n"
            + "    idxQueryRes.put((Long)result.element.id(), score);\n"
            + "    maxScoreForRawIdx = Math.max(maxScoreForRawIdx, score);\n"
            + "  }\n"
            + "  \n"
            + "  idxQueryRes\n");

    assertNotNull("Data not NULL expected", data);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Metadata.Type.Person.Natural',eq('Person.Natural')).count()");
    Integer numItemsWithGUID = Integer.parseInt(JsonPath.read(data, "$.result.data['@value'][0]"));
    assertEquals("11 people records expected", 11, (int) numItemsWithGUID);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE()");
    //    actualRes = JsonPath.read(data, "$.result.data['@value'][0]['@value']");
    assertNotNull("Data not NULL expected", data);

    String data2 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE().count()");
    Integer numEdges = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 45%", 5, (int) numEdges);
    data2 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN DAILEY')).bothE().count()");
    numEdges = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 90%", 5, (int) numEdges);

  }

  @Test public void testMatchingScores90Pcnt() throws Exception
  {
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");

    // This should fall short of the 90% threshold, so no edges should be created.
    Bindings attribs = getBulkRecordAttribs(result);
    attribs.put("pg_nlp_res_person", "[\"John\"]");

    String data = runBulkRequestQuery(attribs,
        String.format(queryStr2, "90.0"));
    String actualRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(actualRes);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Metadata.Type.Person.Natural',eq('Person.Natural')).count()");
    Integer numItemsWithGUID = Integer.parseInt(JsonPath.read(data, "$.result.data['@value'][0]"));
    assertEquals("11 people records expected", 11, (int) numItemsWithGUID);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE()");

    //    actualRes = JsonPath.read(data, "$.result.data['@value'][0]['@value']");
    assertNotNull("Data not NULL expected", data);

    String  data2 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE().count()");
    Integer numEdges = Integer.parseInt( JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 90%", 5, (int) numEdges);

    data2 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN DAILEY')).bothE().count()");
    numEdges = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 90%", 5, (int) numEdges);

  }

  @Test public void testMatchingScores45Pcnt() throws Exception
  {
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");

    // This should fall within the 45% threshold, so an extra  edge should be created.
    Bindings attribs = getBulkRecordAttribs(result);
    attribs.put("pg_nlp_res_person", "[\"John\"]");

    String data = runBulkRequestQuery(attribs,
        String.format(queryStr2, "45.0"));

    String actualRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(actualRes);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Metadata.Type.Person.Natural',eq('Person.Natural')).count()");
    Integer numItemsWithGUID = Integer.parseInt(JsonPath.read(data, "$.result.data['@value'][0]"));
    assertEquals("11 people records expected", 11, (int) numItemsWithGUID);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE()");
    //    actualRes = JsonPath.read(data, "$.result.data['@value'][0]['@value']");
    assertNotNull("Data not NULL expected", data);

    String data2 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE().count()");
    Integer numEdges = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 45%", 5, (int) numEdges);

    String data3  = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN DAILEY')).bothE().count()");
    numEdges = Integer.parseInt(JsonPath.read(data3, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 45%, and JOHN DAILEY is also a  match", 5,
        (int) numEdges);

  }

  @Test public void testMatchingScoresJohnSmith() throws Exception
  {
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");

    Thread.sleep(10000);
    // This should fall short of the 95% threshold, so no edges should be created.
    Bindings attribs = getBulkRecordAttribs(result);
    attribs.put("pg_nlp_res_person", "[\"John Smith\"]");

    String data  = runBulkRequestQuery(attribs,
        String.format(queryStr2, "45.0"));
    String actualRes = JsonPath.read(data, "$.result.data['@value'][0]");
    assertNotNull(actualRes);

    data = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE()");

    //    Integer numItemsWithGUID = JsonPath.read(data, "$.result.data['@value'][0]['@value']");
    //    assertEquals("11 people records expected", 11, (int) numItemsWithGUID);
    assertNotNull(data);

    String data2 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE().has('fromScorePercent',gt((double)90.0)).count()");

    Integer numEdges = Integer.parseInt(JsonPath.read(data2, "$.result.data['@value'][0]"));
    assertEquals("5 people edges expected, as the threshold is 45%", 5, (int) numEdges);

    String data3 = runBulkRequestQuery(attribs,
        "g.V().has('Person.Natural.Full_Name',eq('JOHN SMITH')).bothE().count()");

    Integer allEdges = Integer.parseInt(JsonPath.read(data3, "$.result.data['@value'][0]"));

    assertEquals("5 people edges expected", 5, (int) allEdges);



  }

  @Test public void testMatching() throws Exception
  {
    List<MockFlowFile> result = testCSVRecordsCommon("phase1.csv");

    Bindings attribs = getBulkRecordAttribs(result);
    attribs.put("pg_nlp_res_person", "[\"John Smith\",\"John Dailey\"]");

    String data2 = runBulkRequestQuery(attribs,
        String.format(queryStr2, "45.0"));

    String actualRes = JsonPath.read(data2, "$.result.data['@value'][0]");
    assertNotNull(actualRes);

  }

}


package com.pontusvision.nifi.processors;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.csv.CSVReader;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Test;

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

import static org.junit.Assert.assertNotNull;

public class TestIngestionProcessorDockerComposeRemoteUri
{

  protected static final String TEST_DATA_RESOURCE_DIR = "csv-data/";

  protected TestRunner runner;
  PontusTinkerPopRemoteClientURLQueryStr    ptpc;



  @Test
  public void test1Plus1Query() throws Exception
  {
    ptpc = new PontusTinkerPopRemoteClientURLQueryStr();
    ptpc.enableStopDuringTestsHook = false;

    PropertyDescriptor PROP_IS_EMBEDDED_SERVER;
    PropertyDescriptor PROP_CONF_URI;
    PropertyDescriptor PROP_CONF_QUERY;

    PROP_CONF_QUERY = ptpc.getPropertyDescriptor("Tinkerpop Query");

    ClassLoader testClassLoader = TestIngestionProcessorDockerComposeRemoteUri.class.getClassLoader();
    URL         url             = testClassLoader.getResource("graphdb-conf/client.yml");
    URL         queryUrl             = testClassLoader.getResource("graphdb-conf/query.sample");

    runner = TestRunners.newTestRunner(ptpc);
    runner.setValidateExpressionUsage(true);
    runner.setProperty(PROP_CONF_QUERY, queryUrl.toURI().toString());

    ptpc.useEmbeddedServer = false;
    ptpc.queryAttribPrefixStr = "pg_";
    ptpc.queryStr = queryUrl.toURI().toString();
//
//    runner.enqueue("");
//    runner.run();

    String query = ptpc.getQueryStr(null);
    Assert.assertEquals("1+1", query);


  }



}


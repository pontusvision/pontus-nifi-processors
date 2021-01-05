/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pontusvision.nifi.processors;


import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author phillip
 */
public class TestCleanCSVAccentHeader {
  final static String accentHeader = "Identificação do Processo,Responsável pelo Processo de Negócio.\n" +
      "Mandatório,Mandatório\n";

  /**
   * Test of onTrigger method RoPA headers
   */

  public static void testAccentHeadersHelper(String mergeStrategy, String expectedRes) {
    // Content to be mock a json file
    InputStream header = new ByteArrayInputStream(accentHeader.getBytes());

    // Generate a test runner to mock a processor in a flow
    CleanCSVHeader throttle = new CleanCSVHeader();
    TestRunner runner = TestRunners.newTestRunner(throttle);


    // Add properties

    runner.setProperty(CleanCSVHeader.MULTI_LINE_HEADER_MERGE_STRATEGY, mergeStrategy);
    runner.setProperty(CleanCSVHeader.MULTI_LINE_HEADER_COUNT, "1");
    runner.setProperty(CleanCSVHeader.USE_REGEX, "true");
    runner.setProperty(CleanCSVHeader.REMOVE_ACCENTS, "true");
    runner.setProperty(CleanCSVHeader.CSV_FIND_TEXT, "[\\.| ]");
    runner.setProperty(CleanCSVHeader.CSV_REPLACE_TEXT, "");
    runner.setProperty(CleanCSVHeader.CSV_PROCESSOR_FORMAT, "CUSTOM");

    runner.setValidateExpressionUsage(true);
    // Add the content to the runner
    runner.enqueue(header);

    runner.run(1);

    List<MockFlowFile> headerResults = runner.getFlowFilesForRelationship(CleanCSVHeader.SUCCESS);


    assertEquals("1 flow file", 1, headerResults.size());
    headerResults.get(0).assertContentEquals(expectedRes);


    headerResults.clear();
    runner.enqueue(header);


  }

  @org.junit.Test
  public void testAccentHeaderReplaceStrategy()  {
    testAccentHeadersHelper("REPLACE",
        "pg_IdentificacaodoProcesso,pg_ResponsavelpeloProcessodeNegocio\n" +
            "Mandatório,Mandatório\n");

  }

  @org.junit.Test
  public void testAccentHeaderColNumStrategy()  {
    testAccentHeadersHelper("COL_NUM",
        "pg_1,pg_2\n" +
            "Mandatório,Mandatório\n");

  }

}

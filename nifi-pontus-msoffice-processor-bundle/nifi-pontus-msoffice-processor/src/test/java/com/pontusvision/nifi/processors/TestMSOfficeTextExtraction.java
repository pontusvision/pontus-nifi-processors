/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pontusvision.nifi.processors;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author phillip
 */
public class TestMSOfficeTextExtraction
{

  /**
   * Test of onTrigger method, of class JsonProcessor.
   */
  @org.junit.Test public void testOnTriggerDocx() throws IOException, InitializationException
  {
    // Content to be mock a json file
    PontusMSOfficeTextExtractor throttle = new PontusMSOfficeTextExtractor();

    TestRunner runner = TestRunners.newTestRunner(throttle);

    // Add properties

    //    runner.setProperty(WaitAndBatch.NUM_MESSAGES_TO_READ_TXT,"3");
    //    runner.setProperty(WaitAndBatch.WAIT_TIME_IN_SECONDS_TXT,"5");

    runner.setProperty(PontusMSOfficeTextExtractor.ADD_CONTENT_TO_ATTRIB.getName(), "true");
    runner.setProperty(PontusMSOfficeTextExtractor.ADD_CONTENT_TO_FLOWFILE.getName(), "true");

    // Add the content to the runner
    runner.enqueue(new FileInputStream(new File("src/test/resources/HELLO_WORLD.docx")));

    // Run the enqueued content, it also takes an int = number of contents queued
    runner.run();

    List<MockFlowFile> headerResults = runner.getFlowFilesForRelationship(PontusMSOfficeTextExtractor.PARSED);

    assertTrue("CONTENT_ATTRIB exists", headerResults.get(0).getAttributes().containsKey("CONTENT_ATTRIB"));
    assertTrue("CONTENT_ATTRIB contains HELLO WORLD",
        headerResults.get(0).getAttributes().get("CONTENT_ATTRIB").contains("HELLO WORLD"));

    headerResults.clear();

  }

  /**
   * Test of onTrigger method, of class JsonProcessor.
   */
  @org.junit.Test public void testOnTriggerDoc() throws IOException, InitializationException
  {
    // Content to be mock a json file
    PontusMSOfficeTextExtractor throttle = new PontusMSOfficeTextExtractor();

    TestRunner runner = TestRunners.newTestRunner(throttle);

    // Add properties

    //    runner.setProperty(WaitAndBatch.NUM_MESSAGES_TO_READ_TXT,"3");
    //    runner.setProperty(WaitAndBatch.WAIT_TIME_IN_SECONDS_TXT,"5");

    runner.setProperty(PontusMSOfficeTextExtractor.DOCX_FORMAT.getName(), "false");
    runner.setProperty(PontusMSOfficeTextExtractor.ADD_CONTENT_TO_ATTRIB.getName(), "true");
    runner.setProperty(PontusMSOfficeTextExtractor.ADD_CONTENT_TO_FLOWFILE.getName(), "true");

    // Add the content to the runner
    runner.enqueue(new FileInputStream(new File("src/test/resources/HELLO_WORLD.doc")));

    // Run the enqueued content, it also takes an int = number of contents queued
    runner.run();

    List<MockFlowFile> headerResults = runner.getFlowFilesForRelationship(PontusMSOfficeTextExtractor.PARSED);

    assertTrue("CONTENT_ATTRIB exists", headerResults.get(0).getAttributes().containsKey("CONTENT_ATTRIB"));
    assertTrue("CONTENT_ATTRIB contains HELLO WORLD",
        headerResults.get(0).getAttributes().get("CONTENT_ATTRIB").contains("HELLO WORLD"));

    headerResults.clear();

  }

}

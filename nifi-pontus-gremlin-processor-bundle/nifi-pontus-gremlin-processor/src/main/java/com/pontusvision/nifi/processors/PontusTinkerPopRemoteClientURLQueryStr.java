/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pontusvision.nifi.processors;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.*;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Leo Martins
 */

@EventDriven @SupportsBatching @InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)

@WritesAttributes({ @WritesAttribute(attribute = "reqUUID", description = "UUID from the query"),
    @WritesAttribute(attribute = "pontus.id.type", description = "The type of UUID (NEW, EXISTING)"),
    @WritesAttribute(attribute = "pontus.match.status", description = "The status associated with the record "
        + "(MATCH, POTENTIAL_MATCH, MULTIPLE, MERGE, NO_MATCH) ") })

@Tags({ "pontus", "TINKERPOP", "GREMLIN" }) @CapabilityDescription(
    "Reads data from attributes, and puts it into a Remote TinkerPop Gremlin Server using a given query.  "
        + "Each of the schema fields is passed as a variable to the tinkerpop query string.  "
        + "As an example, if the server has a graph created as g1, then we can create an alias pointing \"g1\" to \"g\" by "
        + "using the alias option.")

public class PontusTinkerPopRemoteClientURLQueryStr extends PontusTinkerPopRemoteClient
{
  protected ExpressionLanguageScope getQueryStrExpressionLanguageScope()
  {
    return ExpressionLanguageScope.FLOWFILE_ATTRIBUTES;
  }

  public String getQueryStr(ProcessContext ctx, Map<String, String> attribs) throws IOException
  {
    String uriStr = queryStr;
    if (getQueryStrExpressionLanguageScope() != ExpressionLanguageScope.NONE)
    {
      uriStr = ctx.getProperty(TINKERPOP_QUERY_STR).evaluateAttributeExpressions(attribs).getValue();
    }


    return IOUtils.toString(new URL(uriStr), Charset.defaultCharset() );



  }
  public String getQueryStr( ProcessSession session) throws IOException
  {
    return IOUtils.toString(new URL(queryStr), Charset.defaultCharset() );
  }



}

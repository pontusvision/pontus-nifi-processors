/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pontusvision.nifi.processors;

import org.apache.nifi.annotation.behavior.*;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.tinkerpop.gremlin.driver.ResultSet;

import javax.script.Bindings;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        + "using the alias option." + "Then, the tinkerpop query looks like this:\n"
        + "'v1 = g.addV(\"person\").property(id, tp_userID1).property(\"name\", tp_userName1).property(\"age\", tp_userAge1).next()\n"
        + "v2 = g.addV(\"software\").property(id, tp_userID2).property(\"name\", tp_userName2).property(\"lang\", tp_userLang2).next()\n"
        + "g.addE(\"created\").from(v1).to(v2).property(id, tp_relId1).property(\"weight\", tp_relWeight1)\n', then the "
        + "variables tp_userID1, tp_userName1, tp_userID2, tp_userName2, tp_userLang2, tp_relId1, tp_relWeight1 would all be taken from the "
        + "flowfile attributes that start with the prefix set in the tinkerpop query parameter prefix (e.g. tp_).")

public class PontusTinkerPopRemoteClientExprLanguageQueryStr extends PontusTinkerPopRemoteClient
{
  protected ExpressionLanguageScope getQueryStrExpressionLanguageScope()
  {
    return ExpressionLanguageScope.FLOWFILE_ATTRIBUTES;
  }


}

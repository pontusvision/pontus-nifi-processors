package com.pontusvision.nifi.processors;


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.reporting.InitializationException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static com.pontusvision.nifi.processors.PontusGetDBMetadataDirect.*;

/**
 * A processor to retrieve a list of tables (and their metadata) from a database connection
 */
@TriggerSerially @Tags({ "sql", "list", "jdbc", "table", "database", "pontus", "metadata",
    "catalogue" }) @CapabilityDescription(
    "Generates a set of flow files, each containing the database names (catalogues) for the given RDBMS. Once "
        + "the catalogue names have been fetched, they will not be fetched again until the Refresh Interval (if set) has elapsed, or until state has been "
        + "manually cleared.") @WritesAttributes({
    @WritesAttribute(attribute = "pg_rdb_db_name", description = "Contains the name of a database table from the connection") }) @Stateful(scopes = {
    Scope.CLUSTER }, description = "After performing a listing of tables, the timestamp of the query is stored. "
    + "This allows the Processor to not re-list tables the next time that the Processor is run. Specifying the refresh interval in the processor properties will "
    + "indicate that when the processor detects the interval has elapsed, the state will be reset and tables will be re-listed as a result. "
    + "This processor is meant to be run on the primary node only.")

public class PontusGetDBCataloguesDirect extends PontusGetDBCatalogues
{

  /*
   * Will ensure that the list of property descriptors is build only once.
   * Will also create a Set of relationships
   */
  static
  {

    Set<Relationship> _relationships = new HashSet<>();
    _relationships.add(REL_SUCCESS);
    _relationships.add(REL_FAILURE);
    relationships = Collections.unmodifiableSet(_relationships);
  }
  @Override protected List<PropertyDescriptor> getSupportedPropertyDescriptors()
  {
    final List<PropertyDescriptor> properties = new ArrayList<>();
    properties.add(DBCP_SERVICE_CONNECTION_URL);
    properties.add(DBCP_SERVICE_CONNECTION_DRIVER_CLASS);
    properties.add(DBCP_SERVICE_CONNECTION_DRIVER_LOCATION);
    properties.add(DBCP_SERVICE_CONNECTION_USER);
    properties.add(DBCP_SERVICE_CONNECTION_PASS);

    return Collections.unmodifiableList(properties);
  }

  @Override  public String getStateMapPropertiesKey(ProcessContext context)
  {
    final String retVal = context.getProperty(DBCP_SERVICE_CONNECTION_URL).getValue();
    return retVal;
  }



  @Override public Connection getConnection(ProcessContext context, FlowFile flowFile)
      throws SQLException, InitializationException
  {
    return getConnectionStatic(context,flowFile);
  }

}
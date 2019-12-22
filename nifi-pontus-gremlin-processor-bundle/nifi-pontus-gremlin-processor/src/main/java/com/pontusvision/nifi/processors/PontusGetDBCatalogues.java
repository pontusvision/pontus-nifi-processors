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
import org.apache.nifi.components.state.StateManager;
import org.apache.nifi.components.state.StateMap;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.StringUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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

public class PontusGetDBCatalogues extends PontusGetDBMetadata
{

  /*
   * Will ensure that the list of property descriptors is build only once.
   * Will also create a Set of relationships
   */
  static
  {

    Set<Relationship> _relationships = new HashSet<>();
    _relationships.add(REL_ORIGINAL);
    _relationships.add(REL_SUCCESS);
    _relationships.add(REL_FAILURE);
    relationships = Collections.unmodifiableSet(_relationships);
  }

  @Override protected List<PropertyDescriptor> getSupportedPropertyDescriptors()
  {
    final List<PropertyDescriptor> properties = new ArrayList<>();
    properties.add(DBCP_SERVICE);

    return Collections.unmodifiableList(properties);
  }

  public ResultSet getQuery(Connection con) throws SQLException
  {
    DatabaseMetaData dbMetaData = con.getMetaData();
    ResultSet        rs         = dbMetaData.getCatalogs();

    return rs;

  }

  public void handleQuery(ResultSet rs, ProcessSession session, FlowFile flowFileOrig) throws SQLException
  {
    while (rs.next())
    {
      final String tableCatalog = rs.getString("TABLE_CAT");

      FlowFile flowFile = session.create(flowFileOrig);

      if (tableCatalog != null)
      {
        flowFile = session.putAttribute(flowFile, DB_TABLE_CATALOG, tableCatalog);
      }


      session.transfer(flowFile, REL_SUCCESS);

    }
  }

  @Override public Set<Relationship> getRelationships()
  {
    return relationships;
  }

  @Override public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException
  {
    final ComponentLog logger = getLogger();
    //    final DBCPService dbcpService = context.getProperty(DBCP_SERVICE).asControllerService(DBCPService.class);
    //    final long refreshInterval = context.getProperty(REFRESH_INTERVAL).asTimePeriod(TimeUnit.MILLISECONDS);

    final FlowFile            flowFileOrig          = session.get();
    final String              stateMapPropertiesKey = getStateMapPropertiesKey(context, flowFileOrig);
    final StateManager        stateManager          = context.getStateManager();
    final StateMap            stateMap;
    final Map<String, String> stateMapProperties;
    try
    {
      stateMap = stateManager.getState(Scope.CLUSTER);
      stateMapProperties = new HashMap<>(stateMap.toMap());
    }
    catch (IOException ioe)
    {
      session.transfer(flowFileOrig, REL_FAILURE);
      throw new ProcessException(ioe);
    }
    boolean refreshTable = true;

    try
    {
      // Refresh state if the interval has elapsed
      long       lastRefreshed = -1;
      final long currentTime   = System.currentTimeMillis();

      if (!StringUtils.isEmpty(stateMapPropertiesKey))
      {
        String lastTimestampForTable = stateMapProperties.get(stateMapPropertiesKey);

        if (!StringUtils.isEmpty(lastTimestampForTable))
        {
          lastRefreshed = Long.parseLong(lastTimestampForTable);
        }
        if (lastRefreshed == -1)
        {
          stateMapProperties.remove(lastTimestampForTable);
        }
        else
        {
          refreshTable = false;
        }
      }
    }
    catch (final NumberFormatException nfe)
    {
      getLogger().error("Failed to retrieve observed last table fetches from the State Manager. Will not perform "
          + "query until this is accomplished.", nfe);
      context.yield();
      session.transfer(flowFileOrig, REL_FAILURE);

      return;
    }
    if (refreshTable)
    {
      try (final Connection con = getConnection(context, flowFileOrig))
      {

        //        DatabaseMetaData dbMetaData = con.getMetaData();
        ResultSet rs = getQuery(con);

        handleQuery(rs, session, flowFileOrig);

        rs.close();
        stateMapProperties.put(stateMapPropertiesKey, Long.toString(System.currentTimeMillis()));


        session.transfer(flowFileOrig, REL_ORIGINAL);
        // Update the timestamps for listed tables
        if (stateMap.getVersion() == -1)
        {
          stateManager.setState(stateMapProperties, Scope.CLUSTER);
        }
        else
        {
          stateManager.replace(stateMap, stateMapProperties, Scope.CLUSTER);
        }

      }
      catch (final SQLException | IOException | InitializationException e)
      {
        session.transfer(flowFileOrig, REL_FAILURE);

        throw new ProcessException(e);
      }

    }
    else
    {
      session.transfer(flowFileOrig, REL_ORIGINAL);

    }

  }
}
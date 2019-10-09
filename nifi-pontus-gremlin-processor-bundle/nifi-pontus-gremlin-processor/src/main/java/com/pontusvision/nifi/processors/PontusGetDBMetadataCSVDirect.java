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

import com.opencsv.CSVWriter;
import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.DatabaseMetaData;
import java.util.LinkedList;
import java.util.List;

/**
 * A processor to retrieve a list of tables (and their metadata) from a database connection
 */
@TriggerSerially @Tags({ "sql", "list", "jdbc", "table", "database", "pontus", "metadata" }) @CapabilityDescription(
    "Same as PontusGetRelationalDBMetadata, but without a controller service, plus adding a CSV version of the column values.  Generates a set of flow files, each containing attributes corresponding to metadata about a table from a database connection. Once "
        + "metadata about a table has been fetched, it will not be fetched again until the Refresh Interval (if set) has elapsed, or until state has been "
        + "manually cleared.") @WritesAttributes({
    @WritesAttribute(attribute = "pg_rdb_table_name", description = "Contains the name of a database table from the connection"),
    @WritesAttribute(attribute = "pg_rdb_table_catalog", description = "Contains the name of the catalog to which the table belongs (may be null)"),
    @WritesAttribute(attribute = "pg_rdb_table_schema", description = "Contains the name of the schema to which the table belongs (may be null)"),
    @WritesAttribute(attribute = "pg_rdb_table_fullname", description = "Contains the fully-qualifed table name (possibly including catalog, schema, etc.)"),
    @WritesAttribute(attribute = "pg_rdb_col_metadata", description = "Contains a JSON representation of the colums metadata."),
    @WritesAttribute(attribute = "pg_rdb_table_type", description =
        "Contains the type of the database table from the connection. Typical types are \"TABLE\", \"VIEW\", \"SYSTEM TABLE\", "
            + "\"GLOBAL TEMPORARY\", \"LOCAL TEMPORARY\", \"ALIAS\", \"SYNONYM\""),
    @WritesAttribute(attribute = "pg_rdb_table_remarks", description = "Contains the name of a database table from the connection"),
    @WritesAttribute(attribute = "pg_rdb_table_count", description = "Contains the number of rows in the table") }) @Stateful(scopes = {
    Scope.CLUSTER }, description = "After performing a listing of tables, the timestamp of the query is stored. "
    + "This allows the Processor to not re-list tables the next time that the Processor is run. Specifying the refresh interval in the processor properties will "
    + "indicate that when the processor detects the interval has elapsed, the state will be reset and tables will be re-listed as a result. "
    + "This processor is meant to be run on the primary node only.")

public class PontusGetDBMetadataCSVDirect extends PontusGetDBMetadataDirect
{

  @Override
  public FlowFile addResultsToFlowFile(
      ProcessSession session,
      FlowFile flowFile,
      JsonObjectBuilder jsonBuilder,
      DatabaseMetaData dbMetaData,
      String tableCatalog,
      String tableSchema,
      String tableName,
      String fqn,
      String tableType,
      String tableRemarks
  ) throws IOException
  {
    flowFile = super
        .addResultsToFlowFile(session, flowFile, jsonBuilder, dbMetaData, tableCatalog, tableSchema, tableName, fqn,
            tableType, tableRemarks);

    JsonObject colMetadata = jsonBuilder.build();
    flowFile = session.putAttribute(flowFile, DB_COL_METADATA, colMetadata.toString());

    String csvPayload = addColDataAsCSV(colMetadata);

    session.write(flowFile, out -> out.write(csvPayload.getBytes()));

    return flowFile;

  }

  public String addColDataAsCSV(JsonObject dbData) throws IOException
  {
    JsonArray colMetaData = dbData.getJsonArray("colMetaData");
/*
            colDetail.add("colName", colName).add("primaryKeyName", (primaryKeyName != null) ? primaryKeyName : "")
                .add("foreignKeyName", (foreignKeyName != null) ? foreignKeyName : "")
                .add("typeName", (typeName!= null)? typeName: "")
                .add("colRemarks", (colRemarks != null) ? colRemarks : "").add("isAutoIncr", isAutoIncr)
                .add("isGenerated", isGenerated).add("octetLen", octetLen).add("ordinalPos", ordinalPos)
                .add("defVal", (defVal != null) ? defVal : "").add("colSize", colSize);
            if (vals != null)
            {
              colDetail.add("vals", vals);
            }

 */
    List<String>       colNames = new LinkedList<>();
    List<List<String>> rows     = new LinkedList<>();
    StringBuilder      sb       = new StringBuilder();

    if (colMetaData != null)
    {
      int ilen = colMetaData.size();
      for (int i = 0; i < ilen; i++)
      {
        JsonValue colDetailValue = colMetaData.get(i);
        if (colDetailValue.getValueType() == JsonValue.ValueType.OBJECT)
        {
          JsonObject colDetailObj = (JsonObject) colDetailValue;
          String     colName      = colDetailObj.getString("colName");
          colNames.add(colName);
          JsonArray rowsForCol = colDetailObj.getJsonArray("vals");

          if (rowsForCol == null){
            for (int j = 0, jlen = this.numRows; j < jlen; j++)
            {
              List<String> row = rows.get(j);
              if (row == null)
              {
                row = new LinkedList<>();
                rows.add(row);
              }
              String val = "";
              row.add(val);
            }

          }
          else
          {
            if (rows.size() < rowsForCol.size())
            {
              for (int j = rows.size(), jlen = rowsForCol.size(); j < jlen; j++)
              {
                rows.add(new LinkedList<>());
              }
            }

            for (int j = 0, jlen = rowsForCol.size(); j < jlen; j++)
            {
              List<String> row = rows.get(j);
              if (row == null)
              {
                row = new LinkedList<>();
                rows.add(row);
              }
              String val = rowsForCol.getString(j);
              row.add(val);
            }
          }

        }

      }

      String colNamesStr = colNames.toString();

      sb.append(colNamesStr, 1, colNamesStr.length() - 1);
      sb.append("\n");
      StringWriter sw = new StringWriter();

      CSVWriter writer = new CSVWriter(sw);

      ilen = rows.size();
      for (int i = 0; i < ilen; i++)
      {
        writer.writeNext(rows.get(i).toArray(new String[0]));
//        String rowStr = rows.get(i).toString();
//
//
//        sb.append(rowStr, 1, rowStr.length() - 1);
//        sb.append("\n");

      }
      writer.close();
      sb.append(sw.toString());

    }

    return sb.toString();

  }

}
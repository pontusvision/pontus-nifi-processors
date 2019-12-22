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

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.file.classloader.ClassLoaderUtils;

import java.net.MalformedURLException;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * A processor to retrieve a list of tables (and their metadata) from a database connection
 */
@TriggerSerially @Tags({ "sql", "list", "jdbc", "table", "database", "pontus", "metadata" }) @CapabilityDescription(
    "Same as PontusGetRelationalDBMetadata, but without a controller service.  Generates a set of flow files, each containing attributes corresponding to metadata about a table from a database connection. Once "
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

public class PontusGetDBMetadataDirect extends PontusGetDBMetadata
{

  // Relationships
//  public static final Relationship REL_SUCCESS = new Relationship.Builder().name("success")
//      .description("All FlowFiles that are received are routed to success").build();

  // Property descriptors
  public static final PropertyDescriptor DBCP_SERVICE_CONNECTION_URL = new PropertyDescriptor.Builder()
      .name("list-db-tables-db-connection-url").displayName("Database Connection URL").addValidator(Validator.VALID)
      .description("The URL that is used to obtain connection to database").required(true)
      .defaultValue("jdbc:mariadb://localhost:3306/").expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).build();

  public static final PropertyDescriptor DBCP_SERVICE_CONNECTION_DRIVER_CLASS = new PropertyDescriptor.Builder()
      .name("list-db-tables-db-connection-driver-class").displayName("Database Driver Class Name").addValidator(Validator.VALID)
      .description("The driver class name that is used to obtain connection to database").required(true)
      .defaultValue("org.mariadb.jdbc.Driver").expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).build();

  public static final PropertyDescriptor DBCP_SERVICE_CONNECTION_DRIVER_LOCATION = new PropertyDescriptor.Builder()
      .name("list-db-tables-db-connection-driver-location").displayName("Database Driver Location(s)").addValidator(Validator.VALID)
      .description("The comma-separated list of jar files (and dependencies) that is used to obtain connection to database").required(true)
      .defaultValue("/usr/share/java/mariadb-java-client-2.3.0.jar").expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).build();

  public static final PropertyDescriptor DBCP_SERVICE_CONNECTION_USER = new PropertyDescriptor.Builder()
      .name("list-db-tables-db-connection-user").displayName("Database Connection User").addValidator(Validator.VALID)
      .description("The User Name  that is used to obtain connection to database").required(true)
      .defaultValue("root").expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).build();

  public static final PropertyDescriptor DBCP_SERVICE_CONNECTION_PASS = new PropertyDescriptor.Builder()
      .name("list-db-tables-db-connection-pass").displayName("Database Connection Password").addValidator(Validator.VALID)
      .description("The Password  that is used to obtain connection to database").required(true)
      .defaultValue("").sensitive(true).expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).build();


  /*
   * Will ensure that the list of property descriptors is build only once.
   * Will also create a Set of relationships
   */
  static
  {
    Set<Relationship> _relationships = new HashSet<>();
    _relationships.add(REL_SUCCESS);
    _relationships.add(REL_FAILURE);
    _relationships.add(REL_ORIGINAL);
    relationships = Collections.unmodifiableSet(_relationships);
  }



  @Override protected List<PropertyDescriptor> getSupportedPropertyDescriptors()
  {

    List<PropertyDescriptor> properties = new ArrayList<>();
    properties.add(DBCP_SERVICE_CONNECTION_URL);
    properties.add(DBCP_SERVICE_CONNECTION_DRIVER_CLASS);
    properties.add(DBCP_SERVICE_CONNECTION_DRIVER_LOCATION);
    properties.add(DBCP_SERVICE_CONNECTION_USER);
    properties.add(DBCP_SERVICE_CONNECTION_PASS);
    properties.add(CATALOG);
    properties.add(SCHEMA_PATTERN);
    properties.add(TABLE_NAME_PATTERN);
    properties.add(COLUMN_NAME_PATTERN);

    properties.add(TABLE_TYPES);
    properties.add(INCLUDE_COUNT);
    properties.add(REFRESH_INTERVAL);
    properties.add(NUM_ROWS);
    return Collections.unmodifiableList(properties);


  }

  @Override public Set<Relationship> getRelationships()
  {
    return relationships;
  }

  public static class DriverShim implements Driver {
    private Driver driver;

    DriverShim(Driver d) {
      this.driver = d;
    }

    @Override
    public boolean acceptsURL(String u) throws SQLException {
      return this.driver.acceptsURL(u);
    }

    @Override
    public Connection connect(String u, Properties p) throws SQLException {
      return this.driver.connect(u, p);
    }

    @Override
    public int getMajorVersion() {
      return this.driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
      return this.driver.getMinorVersion();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
      return this.driver.getPropertyInfo(u, p);
    }

    @Override
    public boolean jdbcCompliant() {
      return this.driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return driver.getParentLogger();
    }

  }
  public static  ClassLoader getDriverClassLoader(String locationString, String drvName) throws InitializationException
  {
    if (locationString != null && locationString.length() > 0) {
      try {
        // Split and trim the entries
        final ClassLoader classLoader = ClassLoaderUtils.getCustomClassLoader(
            locationString,
            PontusGetDBMetadataDirect.class.getClassLoader(),
            (dir, name) -> name != null && name.endsWith(".jar")
        );

        // Workaround which allows to use URLClassLoader for JDBC driver loading.
        // (Because the DriverManager will refuse to use a driver not loaded by the system ClassLoader.)
        final Class<?> clazz = Class.forName(drvName, true, classLoader);
        if (clazz == null) {
          throw new InitializationException("Can't load Database Driver " + drvName);
        }
        final Driver driver = (Driver) clazz.newInstance();
        DriverManager.registerDriver(new DriverShim(driver));

        return classLoader;
      } catch (final MalformedURLException e) {
        throw new InitializationException("Invalid Database Driver Jar Url", e);
      } catch (final Exception e) {
        throw new InitializationException("Can't load Database Driver", e);
      }
    } else {
      // That will ensure that you are using the ClassLoader for you NAR.
      return Thread.currentThread().getContextClassLoader();
    }
  }

  @Override public Connection getConnection(ProcessContext context, FlowFile flowFile)
      throws SQLException, InitializationException
  {
    return getConnectionStatic(context,flowFile);
  }

  public static Connection getConnectionStatic(ProcessContext context, FlowFile flowFile)
      throws SQLException, InitializationException
  {

    final String drv = context.getProperty(DBCP_SERVICE_CONNECTION_DRIVER_CLASS).evaluateAttributeExpressions(flowFile).getValue();
    final String user = context.getProperty(DBCP_SERVICE_CONNECTION_USER).evaluateAttributeExpressions(flowFile).getValue();
    final String passw = context.getProperty(DBCP_SERVICE_CONNECTION_PASS).evaluateAttributeExpressions(flowFile).getValue();

    isOracle = (drv.toLowerCase().contains("oracle"));
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(drv);

    // Optional driver URL, when exist, this URL will be used to locate driver jar file location
    final String urlString = context.getProperty(DBCP_SERVICE_CONNECTION_DRIVER_LOCATION).evaluateAttributeExpressions(flowFile).getValue();
    dataSource.setDriverClassLoader(getDriverClassLoader(urlString, drv));

    final String dburl = context.getProperty(DBCP_SERVICE_CONNECTION_URL).evaluateAttributeExpressions(flowFile).getValue();

    dataSource.setUrl(dburl);
    dataSource.setUsername(user);
    dataSource.setPassword(passw);

    context.getProperties().keySet().stream().filter(PropertyDescriptor::isDynamic)
        .forEach((dynamicPropDescriptor) -> dataSource.addConnectionProperty(dynamicPropDescriptor.getName(),
            context.getProperty(dynamicPropDescriptor).evaluateAttributeExpressions(flowFile).getValue()));


    Connection conn = dataSource.getConnection();

    dataSource.close();
    return  conn;
  }

  public String getStateMapPropertiesKey(ProcessContext context){
    final DBCPService dbcpService = context.getProperty(DBCP_SERVICE).asControllerService(DBCPService.class);
    return dbcpService.toString();
  }


}
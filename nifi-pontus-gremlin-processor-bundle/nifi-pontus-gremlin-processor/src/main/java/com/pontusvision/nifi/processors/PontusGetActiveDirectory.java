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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.keycloak.common.util.KeystoreUtil;

import javax.naming.Context;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A processor to retrieve a list of tables (and their metadata) from a database connection
 */

@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@TriggerSerially
@Tags({ "AD", "Active Directory", "LDAP" })
@CapabilityDescription("Subscribes to Active Directory / LDAP changes.")

public class PontusGetActiveDirectory extends AbstractProcessor
{

  private volatile BlockingQueue<LDAPEvent> ldapEvents;
  private volatile BlockingQueue<LDAPEvent> ldapEventErrors;

  public static final Relationship REL_SUCCESS = new Relationship.Builder().name("success")
                                                                           .description(
                                                                               "FlowFiles that have the LDAP/AD changes")
                                                                           .build();

  // Relationships
  public static final Relationship REL_FAILURE = new Relationship.Builder().name("failure")
                                                                           .description(
                                                                               "Connectivity errors with AD")
                                                                           .build();

  public static final PropertyDescriptor AD_CRED_USER = new PropertyDescriptor.Builder()
      .name("AD_CRED_USER").displayName("Active Directory User Name").addValidator(Validator.VALID)
      .description("The user name to connect to Active Directory").required(true)
      .defaultValue("").sensitive(true).expressionLanguageSupported(true).build();

  public static final PropertyDescriptor AD_CRED_PASS = new PropertyDescriptor.Builder()
      .name("AD_CRED_PASS").displayName("Active Directory Password").addValidator(Validator.VALID)
      .description("The password to connect to Active Directory").required(true)
      .defaultValue("").sensitive(true).expressionLanguageSupported(true).build();

  public static final PropertyDescriptor AD_INITIAL_CONTEXT_FACTORY = new PropertyDescriptor.Builder()
      .name("AD_INITIAL_CONTEXT_FACTORY").displayName("Active Directory Initial Context Factory")
      .addValidator(new ClassNameValidator())
      .description("The class name to load the initial context to connect to Active Directory").required(true)
      .defaultValue("com.sun.jndi.ldap.LdapCtxFactory").sensitive(false).expressionLanguageSupported(false).build();

  public static final PropertyDescriptor AD_PROVIDER_URL = new PropertyDescriptor.Builder()
      .name("AD_PROVIDER_URL").displayName("Active Directory URL")
      .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
      .description("The URL to connect to Active Directory (ldap(s)://<host>:port/<optional distinguished name)")
      .required(true)
      .defaultValue("ldap://localhost:389/o=JNDItutorial").sensitive(false).expressionLanguageSupported(true).build();

  public static final PropertyDescriptor AD_SUBSCRIPTION_DN = new PropertyDescriptor.Builder()
      .name("AD_SUBSCRIPTION_DN")
      .displayName("Active Directory Distinguished Name to subscribe to events")
      .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
      .description("The part of the distinguished name (DN) AD Structure to subscribe for changes").required(true)
      .defaultValue("ou=People").sensitive(false).expressionLanguageSupported(true).build();

  public static final PropertyDescriptor AD_SUBSCRIPTION_FILTER = new PropertyDescriptor.Builder()
      .name("AD_SUBSCRIPTION_FILTER")
      .displayName("Active Directory Filter String")
      .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
      .description("The string to filter subscription results").required(true)
      .defaultValue("(objectclass = *)").sensitive(false).expressionLanguageSupported(true).build();

  public static final PropertyDescriptor AD_SUBSCRIPTION_SCOPE = new PropertyDescriptor.Builder()
      .name("AD_SUBSCRIPTION_SCOPE")
      .displayName("Active Directory Distinguished Name to subscribe to events")
      .allowableValues(SubscritptionScope.values())
      .description("The part of the distinguished name (DN) AD Structure to subscribe for changes").required(true)
      .defaultValue("ONELEVEL_SCOPE").sensitive(false).expressionLanguageSupported(true).build();
  public static final PropertyDescriptor AD_EVENT_QUEUE_SIZE   = new PropertyDescriptor.Builder()
      .name("AD_EVENT_QUEUE_SIZE").displayName("Container Queue Size")
      .description(
          "LDAP events come in a different thread than this NiFi Processor's; as such, there must be a queue in between them.  This property controls the size of that queue.  If the queue fills up, the messages will go into an error queue.")
      .required(true)
      .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR).defaultValue("1000").build();

  //  private        EventContext ctx;
  ASN1OctetString    cookie = null;
  LDAPConnection     connection;
  LDAPConnectionPool pool;
  ExecutorService    executorService;
  AsyncRequestID     asyncSearchId;
  private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  private AsyncSearchResultListener listener    = new AsyncSearchResultListener()
  {
    @Override public void searchEntryReturned(SearchResultEntry searchEntry)
    {
      LDAPEvent event = new LDAPEvent();
      event.searchRes = new SearchResultEntry(searchEntry);
      event.error = null;

      event.name = searchEntry.getDN();

      if (!ldapEvents.offer(event))
      {
        event.error = "Failed to queue LDAP event; queue is full";
        getLogger().error(event.error);
        //noinspection StatementWithEmptyBody
        while (!ldapEventErrors.offer(event))
        { /* loop forever */}
      }

    }

    @Override public void searchReferenceReturned(SearchResultReference searchReference)
    {

    }

    @Override public void searchResultReceived(AsyncRequestID requestID, SearchResult searchResult)
    {

    }
  };
  private boolean                   keepLooping = true;

  public enum SubscritptionScope
  {
    OBJECT_SCOPE, ONELEVEL_SCOPE, SUBTREE_SCOPE
  }

  static Set<Relationship> relationships;

  class LDAPEvent
  {
    public SearchResultEntry searchRes;
    public String            error;
    public String            name;
  }

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

  public static void sendError(ProcessSession session, Exception e)
  {
    sendError(session, e.getMessage());
  }

  public void handleError(ProcessSession session, LDAPEvent e) throws LDAPException
  {
    if (e != null)
    {
      String error = gson.toJson(e, LDAPEvent.class);

      unsubscribeToLDAP();
      sendError(session, error);
    }
  }

  public static void sendError(ProcessSession session, String e)
  {
    forwardFlowFile(session, e, REL_FAILURE);

  }

  public static void handleSuccess(ProcessSession session, LDAPEvent data)
  {
    if (data != null)
    {
      String success = gson.toJson(data, LDAPEvent.class);
      forwardFlowFile(session, success, REL_SUCCESS);
    }

  }

  public static void sendSuccess(ProcessSession session, String data)
  {
    forwardFlowFile(session, data, REL_SUCCESS);

  }

  private static void forwardFlowFile(ProcessSession session, String str, Relationship rel)
  {
    FlowFile flowFile = session.get();
    if (flowFile == null)
    {
      flowFile = session.create();
    }

    flowFile = session.write(
        flowFile,
        out -> {
          if (str != null)
          {
            out.write(str.getBytes());
          }
        });
    session.transfer(flowFile, rel);
  }

  @SuppressWarnings("Duplicates") @Override protected List<PropertyDescriptor> getSupportedPropertyDescriptors()
  {
    List<PropertyDescriptor> props = new ArrayList<>();
    props.add(AD_CRED_USER);
    props.add(AD_CRED_PASS);
    props.add(AD_PROVIDER_URL);
    props.add(AD_SUBSCRIPTION_SCOPE);
    props.add(AD_SUBSCRIPTION_DN);
    props.add(AD_EVENT_QUEUE_SIZE);
    props.add(AD_INITIAL_CONTEXT_FACTORY);
    return Collections.unmodifiableList(props);
  }

  @Override public Set<Relationship> getRelationships()
  {
    return relationships;
  }

  @OnStopped
  public void unsubscribeToLDAP() throws LDAPException
  {
    // Not strictly necessary if we're going to close context anyhow

    if (connection != null)
    {
      keepLooping = false;

      cookie = null;
      executorService.shutdown();

      connection.abandon(asyncSearchId);
      connection.close();
      // Close context when we're done
      connection = null;

    }

  }

  void subscribeToLDAP(ProcessContext context, ProcessSession session)
      throws Exception
  {
    if (connection == null)
    {
      ldapEvents = new LinkedBlockingQueue<>(context.getProperty(AD_EVENT_QUEUE_SIZE).asInteger());
      ldapEventErrors = new LinkedBlockingQueue<>(context.getProperty(AD_EVENT_QUEUE_SIZE).asInteger());
      // Set up environment for creating initial context
      Hashtable<String, Object> env = new Hashtable<>(11);
      env.put(Context.INITIAL_CONTEXT_FACTORY,
          context.getProperty(AD_INITIAL_CONTEXT_FACTORY).getValue());

      env.put(Context.PROVIDER_URL,
          context.getProperty(AD_PROVIDER_URL).evaluateAttributeExpressions(session.get()).getValue());
      env.put(Context.SECURITY_CREDENTIALS,
          context.getProperty(AD_CRED_PASS).evaluateAttributeExpressions(session.get()).getValue());
      env.put(Context.SECURITY_PRINCIPAL,
          context.getProperty(AD_CRED_USER).evaluateAttributeExpressions(session.get()).getValue());

      // Get event context for registering listener
      String subscriptionStr = context.getProperty(AD_SUBSCRIPTION_DN).evaluateAttributeExpressions(session.get())
                                      .getValue();

      String bindDN       = env.get(Context.SECURITY_PRINCIPAL).toString();
      String bindPassword = env.get(Context.SECURITY_CREDENTIALS).toString();

      String urlStr = env.get(Context.PROVIDER_URL).toString();

      URI ldapURI = new URI(urlStr);

      String serverAddress = ldapURI.getHost();
      int    serverPort    = ldapURI.getPort();

      if ("ldaps".equalsIgnoreCase(ldapURI.getScheme()))
      {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(
            KeystoreUtil.loadKeyStore(System.getProperty("javax.net.ssl.trustStore", "/etc/pki/java/truststore.jks")
                , System.getProperty("javax.net.ssl.trustStorePassword", "changeit")));
        TrustManager[]        trustManagers     = tmf.getTrustManagers();
        SSLUtil               sslUtil           = new SSLUtil(trustManagers);
        SSLSocketFactory      socketFactory     = sslUtil.createSSLSocketFactory();
        LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
        connectionOptions.setFollowReferrals(true);

        connection = new LDAPConnection(socketFactory, connectionOptions, serverAddress, serverPort, bindDN,
            bindPassword);

      }
      else
      {
        LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
        connectionOptions.setFollowReferrals(true);
        connection = new LDAPConnection(connectionOptions,
            serverAddress, serverPort, bindDN, bindPassword);

      }
      pool = new LDAPConnectionPool(connection, 1);

      // controller 1.2.840.113556.1.4.528
      String scopeTypeStr = context.getProperty(AD_SUBSCRIPTION_SCOPE)
                                   .evaluateAttributeExpressions(session.get()).getValue();
      SubscritptionScope scopeType   = Enum.valueOf(SubscritptionScope.class, scopeTypeStr);
      SearchScope        searchScope = SearchScope.valueOf(scopeType.ordinal());

      SearchRequest searchRequest = new SearchRequest(
          listener,
          subscriptionStr,  // baseDN
          searchScope,
          Filter.create("ObjectClass=*"), null);

      Control myControl = new Control("1.2.840.113556.1.4.528");
      searchRequest.addControl(myControl);
      asyncSearchId = connection.asyncSearch(searchRequest);

      SearchRequest searchRequestImmediate = new SearchRequest(
          subscriptionStr,  // baseDN
          searchScope,
          //          Filter.createEqualityFilter("objectClass", "*")
          Filter.create("objectclass=*")
          , null);

      SearchResult res = connection.search(searchRequestImmediate);

      for (final SearchResultEntry updatedEntry :
          res.getSearchEntries())
      {
        listener.searchEntryReturned(updatedEntry);
      }

      //      executorService = Executors.newFixedThreadPool(1);

      //      executorService.execute(() -> {
      //
      //        //        final SearchRequest searchRequest = new SearchRequest("dc=example,dc=com",
      //        //            SearchScope.SUB, Filter.createEqualityFilter("objectClass", "User"));
      //
      //        // Define the components that will be included in the DirSync request
      //        // control.
      //
      //        final int flags = ActiveDirectoryDirSyncControl.FLAG_INCREMENTAL_VALUES |
      //            ActiveDirectoryDirSyncControl.FLAG_OBJECT_SECURITY;
      //
      //        // Create a loop that will be used to keep polling for changes.
      //        while (keepLooping)
      //        {
      //          try
      //          {
      //            // Update the controls that will be used for the search request.
      ////            searchRequestImmediate.setControls(new ActiveDirectoryDirSyncControl(true, flags,
      ////                5000, cookie));
      //
      //            // Process the search and get the response control.
      //            asyncSearchId = connection.asyncSearch(searchRequest);
      //
      ////
      ////            ActiveDirectoryDirSyncControl dirSyncResponse =
      ////                ActiveDirectoryDirSyncControl.get(searchResult);
      ////            cookie = dirSyncResponse.getCookie();
      //
      //            // Process the search result entries because they represent entries that
      //            // have been created or modified.
      //            if (asyncSearchId != null)
      //            {
      ////              for (final SearchResultEntry updatedEntry :
      ////                  searchResult.getSearchEntries())
      ////              {
      ////                listener.searchEntryReturned(updatedEntry);
      ////              }
      ////              connection.pr
      //            }
      //            // If the client might want to continue the search even after shutting
      //            // down and starting back up later, then persist the cookie now.
      //          }
      //          catch (LDAPException e)
      //          {
      //            LDAPEvent event = new LDAPEvent();
      //            event.error = e.toString();
      //
      //            event.name = e.getMatchedDN();
      //
      //            if (!ldapEventErrors.offer(event))
      //            {
      //              event.error = "Failed to queue LDAP event; queue is full";
      //              getLogger().error(event.error);
      //              //noinspection StatementWithEmptyBody
      //              while (!ldapEventErrors.offer(event))
      //              { /* loop forever */}
      //            }
      //          }
      //        }
      //
      //        //          pool.processRequests(requests, true);
      //
      //      });

      //      pool.processRequest

      //      SearchResult res = connection.search(searchRequest);
      //      //      LdapCtx retVal = (LdapCtx) ctx.lookup("");
      //      LDAPEvent event = new LDAPEvent();
      //      event.name = res.getMatchedDN();
      //
      //      event.searchRes = res;
      //      //      Attributes retVal = ((LdapCtx) ctx).getAttributes(subscriptionStr);
      //
      //      // Create listener
      //
      //      // Register listener with context (all targets equivalent)
      //      //      ctx.addNamingListener("", scopeTypeNum, listener);
      //
      //      sendSuccess(session, gson.toJson(event, LDAPEvent.class));

    }
  }

  @Override public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException
  {

    try
    {

      subscribeToLDAP(context, session);
      //      while (this.isScheduled())

      try
      {
        LDAPEvent errorEvent = ldapEventErrors.poll();
        handleError(session, errorEvent);

        LDAPEvent event = ldapEvents.poll(2, TimeUnit.MILLISECONDS);
        handleSuccess(session, event);

      }
      catch (InterruptedException e)
      {
        LDAPEvent event = ldapEvents.poll();
        handleSuccess(session, event);

      }

    }
    catch (Exception e)
    {
      sendError(session, e);
    }

  }
}
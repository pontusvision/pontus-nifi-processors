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
import com.sun.jndi.ldap.LdapCtx;
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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;
import java.util.*;
import java.util.concurrent.BlockingQueue;
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

  public static final PropertyDescriptor AD_SUBSCRIPTION = new PropertyDescriptor.Builder()
      .name("AD_SUBSCRIPTION")
      .displayName("Active Directory Distinguished Name to subscribe to events")
      .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
      .description("The part of the distinguished name (DN) AD Structure to subscribe for changes").required(true)
      .defaultValue("ou=People").sensitive(false).expressionLanguageSupported(true).build();

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

  private        EventContext ctx;
  private static Gson         gson = new GsonBuilder().disableHtmlEscaping().create();

  private NamingListener listener = new UnsolicitedNotificationListener()
  {
    @Override public void namingExceptionThrown(NamingExceptionEvent evt)
    {
      LDAPEvent event = new LDAPEvent();
      event.namingExceptionEvent = new NamingExceptionEvent(evt.getEventContext(), evt.getException());
      event.notificationEvent = null;
      try
      {
        event.name = evt.getEventContext().getNameInNamespace();
      }
      catch (NamingException e)
      {
        /* e.printStackTrace(); */
      }

      if (!ldapEventErrors.offer(event))
      {
        event.error = "Failed to queue LDAP error; queue is full";
        getLogger().error(event.error, evt.getException());
        //noinspection StatementWithEmptyBody
        while (!ldapEventErrors.offer(event))
        { /* loop forever */}

      }
    }

    @Override public void notificationReceived(UnsolicitedNotificationEvent evt)
    {
      LDAPEvent event = new LDAPEvent();
      event.notificationEvent = new UnsolicitedNotificationEvent(evt.getSource(), evt.getNotification());
      event.namingExceptionEvent = null;
      event.error = null;

      event.name = evt.getSource().toString();

      if (!ldapEvents.offer(event))
      {
        event.error = "Failed to queue LDAP event; queue is full";
        getLogger().error(event.error);
        //noinspection StatementWithEmptyBody
        while (!ldapEventErrors.offer(event))
        { /* loop forever */}
      }

    }
  };

  public enum SubscritptionScope
  {
    OBJECT_SCOPE, ONELEVEL_SCOPE, SUBTREE_SCOPE
  }

  static Set<Relationship> relationships;

  class LDAPEvent
  {
    NamingExceptionEvent         namingExceptionEvent;
    UnsolicitedNotificationEvent notificationEvent;
    String                       error;
    String                       name;
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

  public void handleError(ProcessSession session, LDAPEvent e) throws NamingException
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
          out.write(str.getBytes());
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
    props.add(AD_SUBSCRIPTION);
    props.add(AD_EVENT_QUEUE_SIZE);
    props.add(AD_INITIAL_CONTEXT_FACTORY);
    return Collections.unmodifiableList(props);
  }

  @Override public Set<Relationship> getRelationships()
  {
    return relationships;
  }

  @OnStopped
  public void unsubscribeToLDAP() throws NamingException
  {
    // Not strictly necessary if we're going to close context anyhow

    if (ctx != null)
    {
      ctx.removeNamingListener(listener);

      // Close context when we're done
      ctx.close();
      ctx = null;
    }

  }

  void subscribeToLDAP(ProcessContext context, ProcessSession session) throws NamingException
  {
    if (ctx == null)
    {
      ldapEvents = new LinkedBlockingQueue<>(context.getProperty(AD_EVENT_QUEUE_SIZE).asInteger());
      ldapEventErrors = new LinkedBlockingQueue<>(context.getProperty(AD_EVENT_QUEUE_SIZE).asInteger());
      // Set up environment for creating initial context
      Hashtable<String, Object> env = new Hashtable<>(11);
      env.put(Context.INITIAL_CONTEXT_FACTORY,
          context.getProperty(AD_INITIAL_CONTEXT_FACTORY).getValue());

      env.put(Context.PROVIDER_URL,
          context.getProperty(AD_PROVIDER_URL).evaluateAttributeExpressions(session.get()).getValue());
      env.put(Context.SECURITY_CREDENTIALS, context.getProperty(AD_CRED_PASS).evaluateAttributeExpressions(session.get()).getValue());
      env.put(Context.SECURITY_PRINCIPAL, context.getProperty(AD_CRED_USER).evaluateAttributeExpressions(session.get()).getValue());

      // Get event context for registering listener
      String subscriptionStr = context.getProperty(AD_SUBSCRIPTION).evaluateAttributeExpressions(session.get())
                                      .getValue();
      ctx = (EventContext)
          (new InitialContext(env).lookup(subscriptionStr));

      String scopeTypeStr = context.getProperty(AD_SUBSCRIPTION_SCOPE)
                                   .evaluateAttributeExpressions(session.get()).getValue();
      SubscritptionScope scopeType    = Enum.valueOf(SubscritptionScope.class, scopeTypeStr);
      int                scopeTypeNum = scopeType.ordinal();
      // Create listener

      // Register listener with context (all targets equivalent)
      ctx.addNamingListener("", scopeTypeNum, listener);

      LdapCtx retVal = (LdapCtx) ctx.lookup("");
      LDAPEvent event = new LDAPEvent();
      event.name = retVal.getNameInNamespace();
      sendSuccess(session, gson.toJson(event,LDAPEvent.class));

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
    catch (NamingException e)
    {
      sendError(session, e);
    }

  }
}
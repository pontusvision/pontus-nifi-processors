/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pontusvision.nifi.processors;

import com.github.trevershick.test.ldap.LdapServerResource;
import com.github.trevershick.test.ldap.annotations.LdapAttribute;
import com.github.trevershick.test.ldap.annotations.LdapConfiguration;
import com.github.trevershick.test.ldap.annotations.LdapEntry;
import com.github.trevershick.test.ldap.annotations.Ldif;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Hashtable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author phillip
 */

@LdapConfiguration(
    bindDn = "cn=Directory Manager",
    password = "mypass",
    port = 11111,
    //    base = @LdapEntry(dn = "dc=myroot",
    //        objectclass = { "top", "domain" }),
    ldifs = @Ldif("/ldap-tests/test.ldiff")
)
public class PontusGetActiveDirectoryTest
{
  class PontusLDAPAttribute implements LdapAttribute{

    private String name;

    public PontusLDAPAttribute(String name, String[] value)
    {
      this.name = name;
      this.value = value;
    }

    private String [] value;


    @Override public String name()
    {
      return name;
    }

    @Override public String[] value()
    {
      return value;
    }

    @Override public Class<? extends Annotation> annotationType()
    {
      return PontusLDAPAttribute.class;
    }
  }

  class PontusLdapEntry implements LdapEntry{

    String dn;
    String[] objectClass;
    LdapAttribute[] attributes;

    public PontusLdapEntry(String dn, String[] objectClass,
                           LdapAttribute[] attributes)
    {
      this.dn = dn;
      this.objectClass = objectClass;
      this.attributes = attributes;
    }

    @Override public String dn()
    {
      return dn;
    }

    @Override public String[] objectclass()
    {
      return objectClass;
    }

    @Override public LdapAttribute[] attributes()
    {
      return attributes;
    }

    @Override public Class<? extends Annotation> annotationType()
    {
      return PontusLdapEntry.class;
    }
  }

  private LdapServerResource server;

  @Before
  public void startup() throws Exception
  {
    server = new LdapServerResource(this).start();
  }

  @After
  public void shutdown()
  {
    server.stop();
  }


  /**
   * Build an LDAP entry from the @LdapEntry annotation
   */
  private Entry entry(LdapEntry ldapEntry) {
    Entry e = new Entry(ldapEntry.dn());
    e.addAttribute("objectClass", ldapEntry.objectclass());
    LdapAttribute[] attrs = ldapEntry.attributes();
    for (int i = 0; attrs != null && i < attrs.length; i++) {
      e.addAttribute(attrs[i].name(), attrs[i].value());
    }
    return e;
  }




  public void changeLDAP(String userId) throws NamingException, IOException, LDAPException
  {
    //    String ldifStr = "";
    //    InputStream ldifStream = new  ByteArrayInputStream(ldifStr.getBytes());
    //
    //    LDIFReader       r         = new LDIFReader(ldifStream);
    //    LDIFChangeRecord readEntry = null;
    //    while ((readEntry = r.readChangeRecord()) != null) {
    //      readEntry.processChange(server);
    //    }

//    LdapEntry entry = new PontusLdapEntry(
//        "cn="+userId+"1,dc=root",
//        new String[]{"person"},
//        new PontusLDAPAttribute[]{
//            new PontusLDAPAttribute("cn",new String[]{userId+"1"}),
//            new PontusLDAPAttribute("userPrincipalName",new String[]{userId+"1"}),
//            new PontusLDAPAttribute("sn",new String[]{userId+"1"})
//
//        });
//
//    server.getServer().add(entry(entry));

    Hashtable<String, Object> env = new Hashtable<>(11);
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap://192.168.99.100:389");
        env.put(Context.SECURITY_PRINCIPAL, "CN=Administrator,CN=Users,DC=pontusvision,DC=com");
    env.put(Context.SECURITY_CREDENTIALS, "pa55wordpa55wordPASSWD999");
//    env.put(Context.PROVIDER_URL, "ldap://localhost:389");
//    env.put(Context.SECURITY_PRINCIPAL, "cn=Directory Manager");
//    env.put(Context.SECURITY_CREDENTIALS, "mypass");
    DirContext ctx =
        (new InitialDirContext(env));

    addEntry(ctx, userId);
    ctx.close();
  }

  public boolean addEntry(DirContext ctx, String userId)
  {
    boolean   flag              = false;
    Attribute userCn            = new BasicAttribute("cn", userId);
    Attribute userSn            = new BasicAttribute("sn", userId);
    Attribute userPrincipalName = new BasicAttribute("userPrincipalName", "test@xxxx.com");
//    Attribute telephoneNumber   = new BasicAttribute("telephoneNumber", "0100000000");
//    Attribute seeAlso           = new BasicAttribute("seeAlso", "testUser");
//    Attribute description       = new BasicAttribute("description", "user for test");
    Attribute userPassword      = new BasicAttribute("userPassword", "password");
    //    Attribute userstatus       = new BasicAttribute("userstatus","A");
    //ObjectClass attributes
//    changetype: add
//    objectclass: person
//    userPassword: thepassword2
//    userPrincipalName: trever.shick
//    sn: Shick
//    cn: tshick

    Attribute oc = new BasicAttribute("objectclass");
    oc.add("person");
    //    oc.add("publicuser");
    //    oc.add("inetOrgPerson");

    Attributes entry = new BasicAttributes();
    entry.put(userCn);
    entry.put(userSn);
    entry.put(userPrincipalName);
//    entry.put(telephoneNumber);
//    entry.put(seeAlso);
//    entry.put(description);
    entry.put(userPassword);
    entry.put(oc);
    //    entry.put(userstatus);
    //uid=142,ou=alzebra,dc=mathsdep,dc=college
    String entryDN = "cn="+userId+",dc=root";
    System.out.println("entryDN :" + entryDN);
    try
    {
      ctx.createSubcontext(entryDN,entry);
      flag = true;
    }
    catch (Exception e)
    {
      System.out.println("error: " + e.getMessage());
      return flag;
    }
    return flag;
  }

  /**
   * Test of onTrigger method, of class JsonProcessor.
   */
  @org.junit.Test public void testOnTrigger()
      throws IOException, InitializationException, NamingException, LDAPException
  {
    // Content to be mock a json file
    InputStream header = new ByteArrayInputStream("header1,header2\n".getBytes());

    // Generate a test runner to mock a processor in a flow
    PontusGetActiveDirectory adMon  = new PontusGetActiveDirectory();
    TestRunner               runner = TestRunners.newTestRunner(adMon);

    runner.setProperty(PontusGetActiveDirectory.AD_CRED_USER, "cn=Directory Manager");
    runner.setProperty(PontusGetActiveDirectory.AD_CRED_PASS, "mypass");
    runner.setProperty(PontusGetActiveDirectory.AD_PROVIDER_URL, "ldap://localhost:11111");
    runner.setProperty(PontusGetActiveDirectory.AD_SUBSCRIPTION, "dc=root");
    runner.setProperty(PontusGetActiveDirectory.AD_SUBSCRIPTION_SCOPE, "SUBTREE_SCOPE");

    runner.setRunSchedule(1000);

    // Add the content to the runner
    //    runner.enqueue(header);
    //    runner.enqueue(header);
    //    void run(int iterations, boolean stopOnFinish, final boolean initialize, final long runWait);
    runner.run(1, false, true, 1000);
    changeLDAP("lmartins");
    runner.run(1, false, true, 10000);

    List<MockFlowFile> headerResults = runner.getFlowFilesForRelationship(PontusGetActiveDirectory.REL_SUCCESS);

    long startTime = System.currentTimeMillis();
    // Run the enqueued content, it also takes an int = number of contents queued
    runner.run(2);

    long deltaTime = System.currentTimeMillis() - startTime;

    assertEquals("6 flow files, because we are waiting for 10 seconds", 6, headerResults.size());

    assertTrue("took greter or equal to 2 * 5 seconds, or 10000 ms", deltaTime >= 10000);

    headerResults.clear();
    runner.enqueue(header);

    runner.setProperty(WaitAndBatch.NUM_MESSAGES_TO_READ_TXT, "15");
    runner.setProperty(WaitAndBatch.WAIT_TIME_IN_SECONDS_TXT, "10");

    runner.run(1);

    headerResults = runner.getFlowFilesForRelationship(WaitAndBatch.SUCCESS);
    assertEquals("9 flow files", 9, headerResults.size());

    runner.setProperty(WaitAndBatch.NUM_MESSAGES_TO_READ_TXT, "15");
    runner.setProperty(WaitAndBatch.WAIT_TIME_IN_SECONDS_TXT, "1");

    runner.run(1);

    headerResults = runner.getFlowFilesForRelationship(WaitAndBatch.SUCCESS);
    assertEquals("9 flow files, as no more messages were enqueued", 9, headerResults.size());

    //    headerResults = runner.getFlowFilesForRelationship(LeakyBucketThrottle.WAITING);
    //
    //    assertTrue(
    //        "Waiting Queue size is still two because we haven't had any more messages arriving to increment the counter.",
    //        headerResults.size() == 2);

  }

}

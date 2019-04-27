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
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Hashtable;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
  class PontusLDAPAttribute implements LdapAttribute
  {

    private String name;

    public PontusLDAPAttribute(String name, String[] value)
    {
      this.name = name;
      this.value = value;
    }

    private String[] value;

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

  class PontusLdapEntry implements LdapEntry
  {

    String          dn;
    String[]        objectClass;
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
  private Entry entry(LdapEntry ldapEntry)
  {
    Entry e = new Entry(ldapEntry.dn());
    e.addAttribute("objectClass", ldapEntry.objectclass());
    LdapAttribute[] attrs = ldapEntry.attributes();
    for (int i = 0; attrs != null && i < attrs.length; i++)
    {
      e.addAttribute(attrs[i].name(), attrs[i].value());
    }
    return e;
  }

  public void changeLDAP(String userId) throws NamingException
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
    env.put(Context.PROVIDER_URL, "ldaps://pontus-sandbox.pontusvision.com:636");
    env.put(Context.SECURITY_PRINCIPAL, "CN=Administrator,CN=Users,DC=pontusvision,DC=com");
    env.put(Context.SECURITY_CREDENTIALS, "pa55wordpa55wordPASSWD999");
    //    env.put(Context.PROVIDER_URL, "ldaps://localhost:636");
    //    env.put(Context.SECURITY_PRINCIPAL, "cn=Directory Manager");
    //    env.put(Context.SECURITY_CREDENTIALS, "mypass");
    DirContext ctx =
        (new InitialDirContext(env));

    try
    {
      addEntry(ctx, userId);
    }
    catch (Exception e)
    {
      //      removeEntry(ctx, userId);
    }
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
    Attribute userPassword = new BasicAttribute("userPassword", "password");
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
    //    entry.put(userPrincipalName);
    //    entry.put(telephoneNumber);
    //    entry.put(seeAlso);
    //    entry.put(description);
    entry.put(userPassword);
    entry.put(oc);
    //    entry.put(userstatus);
    //uid=142,ou=alzebra,dc=mathsdep,dc=college
    String entryDN = "cn=" + userId + ",CN=Users,DC=pontusvision,DC=com";
    System.out.println("entryDN :" + entryDN);
    try
    {
      ctx.createSubcontext(entryDN, entry);
      flag = true;
    }
    catch (Exception e)
    {

      removeEntry(ctx, userId);
      try
      {
        ctx.createSubcontext(entryDN, entry);
      }
      catch (NamingException e1)
      {
        e1.printStackTrace();
      }

      return flag;
    }
    return flag;
  }

  public boolean removeEntry(DirContext ctx, String userId)
  {
    boolean   flag              = false;
    Attribute userCn            = new BasicAttribute("cn", userId);
    Attribute userSn            = new BasicAttribute("sn", userId);
    Attribute userPrincipalName = new BasicAttribute("userPrincipalName", "test@xxxx.com");
    //    Attribute telephoneNumber   = new BasicAttribute("telephoneNumber", "0100000000");
    //    Attribute seeAlso           = new BasicAttribute("seeAlso", "testUser");
    //    Attribute description       = new BasicAttribute("description", "user for test");
    Attribute userPassword = new BasicAttribute("userPassword", "password");
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
    //    entry.put(userPrincipalName);
    //    entry.put(telephoneNumber);
    //    entry.put(seeAlso);
    //    entry.put(description);
    entry.put(userPassword);
    entry.put(oc);
    //    entry.put(userstatus);
    //uid=142,ou=alzebra,dc=mathsdep,dc=college
    String entryDN = "cn=" + userId + ",CN=Users,DC=pontusvision,DC=com";
    System.out.println("entryDN :" + entryDN);
    try
    {
      ctx.destroySubcontext(entryDN);
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
      throws  NamingException
  {
    // Content to be mock a json file
    InputStream header = new ByteArrayInputStream("header1,header2\n".getBytes());

    // Generate a test runner to mock a processor in a flow.
    PontusGetActiveDirectory adMon  = new PontusGetActiveDirectory();
    TestRunner               runner = TestRunners.newTestRunner(adMon);

    runner.setProperty(PontusGetActiveDirectory.AD_CRED_USER, "CN=Administrator,CN=Users,DC=pontusvision,DC=com");
    runner.setProperty(PontusGetActiveDirectory.AD_CRED_PASS, "pa55wordpa55wordPASSWD999");
    runner.setProperty(PontusGetActiveDirectory.AD_PROVIDER_URL, "ldaps://pontus-sandbox.pontusvision.com:636");

    runner.setProperty(PontusGetActiveDirectory.AD_SUBSCRIPTION_DN, "CN=Users,DC=pontusvision,DC=com");
    runner.setProperty(PontusGetActiveDirectory.AD_SUBSCRIPTION_SCOPE, "SUBTREE_SCOPE");

    runner.setRunSchedule(1000);
    runner.run(10, false, true, 10000);
    List<MockFlowFile> headerResults = runner.getFlowFilesForRelationship(PontusGetActiveDirectory.REL_SUCCESS);

    assertEquals("10 flow files", 10, headerResults.size());
    runner.run(10, false, true, 10000);

    headerResults = runner.getFlowFilesForRelationship(PontusGetActiveDirectory.REL_SUCCESS);

    // Add the content to the runner
    //    runner.enqueue(header);
    //    runner.enqueue(header);
    //    void run(int iterations, boolean stopOnFinish, final boolean initialize, final long runWait);
    //    runner.run(1, false, true, 1000);
    changeLDAP("lmartins10");
    try
    {
      Thread.sleep(10000);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
    headerResults = runner.getFlowFilesForRelationship(PontusGetActiveDirectory.REL_SUCCESS);

    runner.run(10, false, true, 10000);

    changeLDAP("lmartins9");
    try
    {
      Thread.sleep(10000);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
    runner.run(10, false, true, 10000);

    headerResults = runner.getFlowFilesForRelationship(PontusGetActiveDirectory.REL_SUCCESS);
//    assertEquals("19 flow files", 19, headerResults.size());

    assertEquals("21 flow files", 21, headerResults.size());

  }

}

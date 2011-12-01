package com.t11e.discovery.datatool;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.dom4j.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JsonColumnsTest
  extends EndToEndTestBase
{

  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("JsonColumnsTest.xml");
  }

  @Before
  public void setup()
  {
    executeSqlScripts("JsonColumnsTestCreate.sql");
  }

  @After
  public void teardown()
  {
    executeSqlScripts("JsonColumnsTestDrop.sql");
  }

  @Test
  public void testDefault()
  {
    final Document doc = assertChangeset("none", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Collections.<String> emptyList(),
      false);

    assertXpath("joe", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='name']/string/text()");
    assertXpath("{'name':'joseph','age':21}".replace('\'', '"'), doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='JSON']/string/text()");
  }

  @Test
  public void legacy()
  {
    testScopedJson("legacy");
  }

  @Test
  public void scoped()
  {
    testScopedJson("scoped");
  }

  private void testScopedJson(final String publisher)
  {
    final Document doc = assertChangeset(publisher, "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Collections.<String> emptyList(),
      false);

    assertXpath("joe", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='name']/string/text()");
    assertXpath("joseph", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='JSON']/struct/entry[@name='name']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='JSON']/struct/entry[@name='age']/int/text()");
  }

  @Test
  public void unscoped()
  {
    final Document doc = assertChangeset("unscoped", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Collections.<String> emptyList(),
      false);

    assertXpath("joseph", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='name']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='age']/int/text()");
  }

  @Test
  public void unscopedOrder()
  {
    final Document doc = assertChangeset("unscoped-order", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Collections.<String> emptyList(),
      false);

    assertXpath("joe", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='name']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='age']/int/text()");
  }

  @Test
  public void mixed()
  {
    final Document doc = assertChangeset("mixed", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Collections.<String> emptyList(),
      false);

    assertXpath("joseph", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='name']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='age']/int/text()");
    assertXpath("joseph", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='JSON']/struct/entry[@name='name']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='JSON']/struct/entry[@name='age']/int/text()");
  }

  @Test
  public void mixedUpper()
  {
    final Document doc = assertChangeset("mixed-upper", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Collections.<String> emptyList(),
      false);

    assertXpath("joseph", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='NAME']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='AGE']/int/text()");
    assertXpath("joseph", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='JSON']/struct/entry[@name='name']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='JSON']/struct/entry[@name='age']/int/text()");
  }

}

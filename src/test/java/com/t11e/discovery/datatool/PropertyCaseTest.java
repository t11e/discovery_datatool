package com.t11e.discovery.datatool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.dom4j.Document;
import org.junit.Test;

public class PropertyCaseTest
  extends EndToEndTestBase
{
  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("PropertyCaseTest.xml");
  }

  @Override
  protected String[] getSetupScripts()
  {
    return new String[]{"PropertyCaseTestCreate.sql",};
  }

  @Override
  protected String[] getCleanupScripts()
  {
    return new String[]{"PropertyCaseTestDrop.sql",};
  }

  @Test
  public void testDefaultPerserve()
  {
    testDefaultPerserve("default");
  }

  private void testDefaultPerserve(final String publisher)
  {
    final Document doc = assertChangeset(publisher, "", "snapshot",
      Arrays.asList("1", "2"),
      Collections.<String> emptyList(),
      false);

    assertXpath("2010-01-01T00:00:00.000", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='lastUpdated']/string/text()");

    assertXpath("red", doc, "/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/string/text()");

    assertXpath("yellow", doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='coloR']/string/text()");
    assertXpath("orange", doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='Color']/string/text()");

    assertXpath(
      "1 Main St",
      doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='street']/string/text()");

    assertNull(doc
      .selectSingleNode("/changeset/set-item[@id='1']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='discriminator']"));

    assertEquals(3,
      doc.selectNodes("/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry").size());

    assertXpath(
      "123 Main St",
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='Biz']/struct/entry[@name='street']/string/text()");

    // biz != Biz
    assertXpath(
      "123 Hidden by second biz address",
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='street']/string/text()");


    assertEquals(
      null,
      doc.selectSingleNode("/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='discriminator']"));

    assertXpath(
      "456 Main St",
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='per']/struct/entry[@name='street']/string/text()");
  }

  @Test
  public void testLower()
  {
    final Document doc = assertChangeset("lower", "", "snapshot",
      Arrays.asList("1", "2"),
      Collections.<String> emptyList(),
      false);

    assertXpath(Arrays.asList("orange", "yellow"),
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='color']/array/element/string/text()");

    assertXpath("2010-01-01T00:00:00.000", doc,
        "/changeset/set-item[@id='1']/properties/struct/entry[@name='lastupdated']/string/text()");

    assertXpath("red", doc, "/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/string/text()");

    assertXpath(
      "1 Main St",
      doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='street']/string/text()");

    assertNull(doc
      .selectSingleNode("/changeset/set-item[@id='1']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='discriminator']"));

    assertEquals(2,
      doc.selectNodes("/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry").size());

    assertXpath(
      "123 Main St",
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='street']/string/text()");

    assertEquals(
      null,
      doc
        .selectSingleNode("/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='discriminator']"));

    assertXpath(
      "456 Main St",
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='per']/struct/entry[@name='street']/string/text()");
  }

  @Test
  public void testUpper()
  {
    final Document doc = assertChangeset("upper", "", "snapshot",
      Arrays.asList("1", "2"),
      Collections.<String> emptyList(),
      false);

    assertXpath(Arrays.asList("orange", "yellow"),
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='COLOR']/array/element/string/text()");

    assertXpath("2010-01-01T00:00:00.000", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='LASTUPDATED']/string/text()");

    assertXpath("red", doc, "/changeset/set-item[@id='1']/properties/struct/entry[@name='COLOR']/string/text()");

    assertXpath(
      "1 Main St",
      doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='ADDRESS']/struct/entry[@name='BIZ']/struct/entry[@name='STREET']/string/text()");

    assertNull(doc
      .selectSingleNode("/changeset/set-item[@id='1']/properties/struct/entry[@name='ADDRESS']/struct/entry[@name='BIZ']/struct/entry[@name='DISCRIMINATOR']"));

    assertEquals(2,
      doc.selectNodes("/changeset/set-item[@id='2']/properties/struct/entry[@name='ADDRESS']/struct/entry").size());

    assertXpath(
      "123 Main St",
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='ADDRESS']/struct/entry[@name='BIZ']/struct/entry[@name='STREET']/string/text()");

    assertEquals(
      null,
      doc
        .selectSingleNode("/changeset/set-item[@id='2']/properties/struct/entry[@name='ADDRESS']/struct/entry[@name='BIZ']/struct/entry[@name='DISCRIMINATOR']"));

    assertXpath(
      "456 Main St",
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='ADDRESS']/struct/entry[@name='PER']/struct/entry[@name='STREET']/string/text()");
  }

  @Test
  public void testPerserve()
  {
    testDefaultPerserve("preserve");
  }
}

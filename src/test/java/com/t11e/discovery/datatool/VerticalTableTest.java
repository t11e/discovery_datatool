package com.t11e.discovery.datatool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.dom4j.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VerticalTableTest
  extends EndToEndTestBase
{
  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("VerticalTableTest.xml");
  }

  @Before
  public void setup()
  {
    executeSqlScripts("VerticalTableTestCreate.sql");
  }

  @After
  public void teardown()
  {
    executeSqlScripts("VerticalTableTestDrop.sql");
  }

  @Test
  public void testSnapshot()
  {
    final Document doc = assertChangeset("test-simple", "", "snapshot",
      Arrays.asList("1", "2"),
      Arrays.asList("4", "5"),
      false);
    assertEquals("red",
      doc.selectSingleNode(
        "/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());

    assertEquals(
      Arrays.asList("orange", "yellow"),
      nodesAsStrings(doc,
        "/changeset/set-item[@id='2']/properties/struct/entry[@name='color']/array/element/string/text()"));
  }

  @Test
  public void testSnapshotWithSubquery()
  {
    final Document doc = assertChangeset("test-subquery", "", "bulk",
      Arrays.asList("1", "2"),
      Collections.<String> emptySet(),
      false);
    assertEquals("red",
      doc.selectSingleNode(
        "/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());

    assertEquals(
      Arrays.asList("orange", "yellow"),
      nodesAsStrings(doc,
        "/changeset/set-item[@id='2']/properties/struct/entry[@name='color']/array/element/string/text()"));

    assertEquals(
      "1 main st",
      doc
        .selectSingleNode(
          "/changeset/set-item[@id='1']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='street']/string/text()")
        .getText());
    assertNull(doc.selectSingleNode(
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='discriminator']"));

    assertEquals(2,
      doc.selectNodes("/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry").size());

    assertEquals(
      "123 main st",
      doc
        .selectSingleNode(
          "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='street']/string/text()")
        .getText());

    assertNull(
      doc
      .selectSingleNode(
        "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='discriminator']"));

    assertEquals(
      "456 main st",
      doc
        .selectSingleNode(
          "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='per']/struct/entry[@name='street']/string/text()")
        .getText());
  }
}

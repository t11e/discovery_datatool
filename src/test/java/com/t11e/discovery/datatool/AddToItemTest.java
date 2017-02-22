package com.t11e.discovery.datatool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.dom4j.Document;
import org.junit.Test;

public class AddToItemTest
  extends EndToEndTestBase
{
  @Override
  protected String[] getSetupScripts()
  {
    return new String[]{"VerticalTableTestCreate.sql", "changeset_profile_create.sql"};
  }

  @Override
  protected String[] getCleanupScripts()
  {
    return new String[]{"VerticalTableTestDrop.sql", "changeset_profile_drop.sql"};
  }

  @Test
  public void testDelta()
  {
    final Document doc = assertChangeset("test-simple", newProfile(),
      "delta",
      Arrays.asList("1", "2", "3"),
      Arrays.asList("4", "5"),
      Arrays.asList("1", "2"),
      false);
    assertEquals("red",
      doc.selectSingleNode(
        "/changeset/add-to-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());
    assertEquals(
      Arrays.asList("orange", "yellow"),
      nodesAsStrings(doc,
        "/changeset/add-to-item[@id='2']/properties/struct/entry[@name='color']/array/element/string/text()"));
  }

  @Test
  public void testSnapshot()
  {
    final Document doc = assertChangeset("test-simple", "",
      "snapshot",
      Arrays.asList("1", "2", "3"),
      Collections.<String> emptyList(),
      Arrays.asList("1", "2"),
      true);
    assertEquals("red",
      doc.selectSingleNode(
        "/changeset/add-to-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());
    assertEquals(
      Arrays.asList("orange", "yellow"),
      nodesAsStrings(doc,
        "/changeset/add-to-item[@id='2']/properties/struct/entry[@name='color']/array/element/string/text()"));
  }

  @Test
  public void testDeltaProvider()
  {
    final Document doc = assertChangeset("test-provider", newProfile(), "delta", false);
    assertEquals(
      "red",
      doc
        .selectSingleNode(
          "/changeset/add-to-item[@locator='1' and @provider='p1' and @kind='k1']/properties/struct/entry[@name='color']/string/text()")
        .getText());

    assertEquals(
      Arrays.asList("orange", "yellow"),
      nodesAsStrings(
        doc,
        "/changeset/add-to-item[@locator='2' and @provider='' and @kind='']/properties/struct/entry[@name='color']/array/element/string/text()"));
  }

  @Test
  public void testDeltaWithSubquery()
  {
    final Document doc = assertChangeset("test-subquery", newProfile(), "delta",
      Collections.<String> emptySet(),
      Collections.<String> emptySet(),
      Arrays.asList("1", "2"),
      false);

    assertEquals("red",
      doc.selectSingleNode(
        "/changeset/add-to-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());

    assertEquals(
      Arrays.asList("orange", "yellow"),
      nodesAsStrings(doc,
        "/changeset/add-to-item[@id='2']/properties/struct/entry[@name='color']/array/element/string/text()"));

    assertEquals(
      "1 main st",
      doc
        .selectSingleNode(
          "/changeset/add-to-item[@id='1']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='STREET']/string/text()")
        .getText());
    assertNull(doc
      .selectSingleNode(
      "/changeset/add-to-item[@id='1']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='DISCRIMINATOR']"));

    assertEquals(2,
      doc.selectNodes("/changeset/add-to-item[@id='2']/properties/struct/entry[@name='address']/struct/entry").size());

    assertEquals(
      "123 main st",
      doc
        .selectSingleNode(
          "/changeset/add-to-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='STREET']/string/text()")
        .getText());

    assertNull(doc
      .selectSingleNode(
      "/changeset/add-to-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='biz']/struct/entry[@name='DISCRIMINATOR']"));

    assertEquals(
      "456 main st",
      doc
        .selectSingleNode(
          "/changeset/add-to-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='per']/struct/entry[@name='STREET']/string/text()")
        .getText());
  }

  @Test
  public void deltaOnly()
  {
    assertChangeset("test-delta-only-set-add-remove", null, "snapshot",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(),
      Collections.<String> emptyList(),
      false);

    assertChangeset("test-delta-only-set-add-remove", null, "snapshot",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(),
      Collections.<String> emptyList(),
      true);
  }

  @Test
  public void deltaOnlyWithProfile()
  {
    final Document doc = assertChangeset("test-delta-only-set-add-remove", newProfile(), "delta",
      Arrays.asList("1", "2", "3"),
      Arrays.asList("4", "5"),
      Arrays.asList("1", "2", "3"),
      false);
    assertXpath("2010-01-01T00:00:00.000", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='LASTUPDATED']/string/text()");
    assertXpath("2010-01-01T00:00:00.000", doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='LASTUPDATED']/string/text()");
    assertXpath("2010-01-01T00:00:00.000", doc,
      "/changeset/set-item[@id='3']/properties/struct/entry[@name='LASTUPDATED']/string/text()");
    assertXpath("billy", doc,
      "/changeset/add-to-item[@id='1']/properties/struct/entry[@name='FIRST_NAME']/string/text()");
    assertXpath("joe", doc,
      "/changeset/add-to-item[@id='2']/properties/struct/entry[@name='FIRST_NAME']/string/text()");
    assertXpath("bob", doc,
      "/changeset/add-to-item[@id='3']/properties/struct/entry[@name='FIRST_NAME']/string/text()");
  }

  @Test
  public void snapshotOnly()
  {
    final Document doc = assertChangeset("test-snapshot-only-set-add-remove", null, "snapshot",
      Arrays.asList("1", "2", "3"),
      Arrays.asList("4", "5"),
      Arrays.asList("1", "2", "3"),
      false);
    assertXpath("2010-01-01T00:00:00.000", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='LASTUPDATED']/string/text()");
    assertXpath("2010-01-01T00:00:00.000", doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='LASTUPDATED']/string/text()");
    assertXpath("2010-01-01T00:00:00.000", doc,
      "/changeset/set-item[@id='3']/properties/struct/entry[@name='LASTUPDATED']/string/text()");
    assertXpath("billy", doc,
      "/changeset/add-to-item[@id='1']/properties/struct/entry[@name='First_name']/string/text()");
    assertXpath("joe", doc,
      "/changeset/add-to-item[@id='2']/properties/struct/entry[@name='First_name']/string/text()");
    assertXpath("bob", doc,
      "/changeset/add-to-item[@id='3']/properties/struct/entry[@name='First_name']/string/text()");
  }

  private static String newProfile()
  {
    return "test-" + new Random(System.currentTimeMillis()).nextLong();
  }
}

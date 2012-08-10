package com.t11e.discovery.datatool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.dom4j.Document;
import org.junit.Test;

public class PropertyCaseLegacyTest
  extends EndToEndTestBase
{
  @Override
  protected String[] getSetupScripts()
  {
    return new String[]{"PropertyCaseTestCreate.sql"};
  }

  @Override
  protected String[] getCleanupScripts()
  {
    return new String[]{"PropertyCaseTestDrop.sql"};
  }

  @Test
  public void testLegacy()
  {
    final Document doc = assertChangeset("default", "", "snapshot",
      Arrays.asList("1", "2"),
      Collections.<String> emptyList(),
      false);

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

    // Note Biz != biz
    assertXpath(
      "123 Main St",
      doc,
      "/changeset/set-item[@id='2']/properties/struct/entry[@name='address']/struct/entry[@name='Biz']/struct/entry[@name='street']/string/text()");

    assertXpath(
      "123 Hidden by second biz address",
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
}

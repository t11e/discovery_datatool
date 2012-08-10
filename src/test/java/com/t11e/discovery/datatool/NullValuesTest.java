package com.t11e.discovery.datatool;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.dom4j.Document;
import org.junit.Test;

public class NullValuesTest
  extends EndToEndTestBase
{
  @Test
  public void nulls()
  {
    final Document doc = assertChangeset("null_values", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Collections.<String> emptyList(),
      false);
    final String asXml = doc.asXML();

    assertXpath("joseph", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='name']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='age']/int/text()");
    assertEquals(asXml, 1,
      doc.selectNodes("/changeset/set-item[@id='1']/properties/struct/entry[@name='price']").size());
    assertEquals(asXml, 0,
      doc.selectNodes("/changeset/set-item[@id='3']/properties/struct/entry[@name='name']").size());
  }

  @Test
  public void provdierNulls()
  {
    final Document doc = assertChangeset("null_values_provider", "", "snapshot", false);
    final String asXml = doc.asXML();
    assertEquals(asXml, 3, doc.selectNodes("/changeset/set-item").size());
    assertEquals(asXml, 0, doc.selectNodes("/changeset/set-item[@id]").size());
    assertEquals(asXml, 3, doc.selectNodes("/changeset/set-item[@locator and @provider and @kind]").size());

    assertXpath("joseph", doc,
      "/changeset/set-item[@locator='1']/properties/struct/entry[@name='name']/string/text()");
    assertXpath("21", doc,
      "/changeset/set-item[@locator='1']/properties/struct/entry[@name='age']/int/text()");
    assertXpath(Collections.<String> emptyList(), doc,
      "/changeset/set-item[@locator='1']/properties/struct/entry[@name='price']/text()");
    assertEquals(asXml, 1,
      doc.selectNodes("/changeset/set-item[@locator='1']/properties/struct/entry[@name='price']").size());
    assertEquals(asXml, 0,
      doc.selectNodes("/changeset/set-item[@locator='3']/properties/struct/entry[@name='name']").size());
  }
}

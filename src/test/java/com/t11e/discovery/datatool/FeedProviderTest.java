package com.t11e.discovery.datatool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.dom4j.Document;
import org.dom4j.Node;
import org.junit.Test;

public class FeedProviderTest
  extends EndToEndTestBase
{
  @Test
  public void simple()
  {
    final Document doc = assertChangeset("test-simple", "", "snapshot", false);
    final String asXml = doc.asXML();
    assertEquals(asXml, 4, doc.selectNodes("/changeset/set-item").size());
    assertEquals(asXml, 0, doc.selectNodes("/changeset/set-item[@id]").size());
    assertEquals(asXml, 4, doc.selectNodes("/changeset/set-item[@locator and @provider and @kind]").size());
    {
      final Node node = doc.selectSingleNode("/changeset/set-item[@locator='1' and @provider='' and @kind='']");
      assertNotNull(node);
      assertEquals("val1", node.selectSingleNode("./properties/struct/entry[@name='value']/string/text()").getText());
      assertNoIdProviderOrKindProperties(node);
    }
    {
      final Node node = doc.selectSingleNode("/changeset/set-item[@locator='2' and @provider='p1' and @kind='k1']");
      assertNotNull(node);
      assertEquals("val2", node.selectSingleNode("./properties/struct/entry[@name='value']/string/text()").getText());
      assertNoIdProviderOrKindProperties(node);
    }
    {
      final Node node = doc.selectSingleNode("/changeset/set-item[@locator='3' and @provider='p2' and @kind='k2']");
      assertNotNull(node);
      assertEquals("val3", node.selectSingleNode("./properties/struct/entry[@name='value']/string/text()").getText());
      assertNoIdProviderOrKindProperties(node);
    }
    {
      final Node node = doc.selectSingleNode("/changeset/set-item[@locator='4' and @provider='' and @kind='']");
      assertNotNull(node);
      assertEquals("val4", node.selectSingleNode("./properties/struct/entry[@name='value']/string/text()").getText());
      assertNoIdProviderOrKindProperties(node);
    }
    assertEquals(asXml, 2, doc.selectNodes("/changeset/remove-item").size());
    assertEquals(asXml, 0, doc.selectNodes("/changeset/remove-item[@id]").size());
    assertEquals(asXml, 2, doc.selectNodes("/changeset/remove-item[@locator and @provider and @kind]").size());
    assertNotNull(doc.selectSingleNode("/changeset/remove-item[@locator='5' and @provider='p1' and @kind='k1']"));
    assertNotNull(doc.selectSingleNode("/changeset/remove-item[@locator='6' and @provider='' and @kind='']"));
  }

  private void assertNoIdProviderOrKindProperties(final Node node)
  {
    assertEquals(0,
      node.selectNodes("./properties/struct/entry[@name='id' or @name='kind' or @name='provider']").size());
  }

  @Test
  public void muliProvider()
  {
    final Document doc = assertChangeset("test-multi", "", "snapshot", false);
    final String asXml = doc.asXML();
    assertEquals(asXml, 12, doc.selectNodes("/changeset/set-item").size());
    assertEquals(asXml, 4, doc.selectNodes("/changeset/set-item[@id]").size());
    assertEquals(asXml, 8, doc.selectNodes("/changeset/set-item[@locator and @provider and @kind]").size());
    assertEquals(0,
      doc.selectNodes("//properties/struct/entry[@name='id' or @name='kind' or @name='provider']").size());

    assertEquals(asXml, 6, doc.selectNodes("/changeset/remove-item").size());
    assertEquals(asXml, 2, doc.selectNodes("/changeset/remove-item[@id]").size());
    assertEquals(asXml, 4, doc.selectNodes("/changeset/remove-item[@locator and @provider and @kind]").size());
    assertNotNull(doc.selectSingleNode("/changeset/remove-item[@locator='5' and @provider='p1' and @kind='k1']"));
    assertNotNull(doc.selectSingleNode("/changeset/remove-item[@locator='6' and @provider='' and @kind='']"));
    assertNotNull(doc.selectSingleNode("/changeset/remove-item[@locator='5' and @provider='calcp' and @kind='calck']"));
    assertNotNull(doc.selectSingleNode("/changeset/remove-item[@locator='6' and @provider='calcp' and @kind='calck']"));
    assertNotNull(doc.selectSingleNode("/changeset/remove-item[@id='5']"));
    assertNotNull(doc.selectSingleNode("/changeset/remove-item[@id='6']"));

  }
}

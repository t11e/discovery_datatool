package com.t11e.discovery.datatool;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Assert;
import org.junit.Test;

public class Dom4JTest
{
  @Test
  public void testCharacterReading()
    throws DocumentException
  {
    final SAXReader saxReader = new SAXReader();
    final Document doc =
      saxReader.read(getClass().getResourceAsStream("Dom4JTest.xml"));
    Assert.assertEquals(
      "abc",
      doc.valueOf("/test/one-line"));
    Assert.assertEquals(
      "\nabc\ndef",
      doc.valueOf("/test/two-lines"));
    Assert.assertEquals(
      "\nThere was a young rustic named Mallory,\n" +
      "who drew but a very small salary.\n" +
      "When he went to the show,\n" +
      "his purse made him go\n" +
      "to a seat in the uppermost gallery.\n" +
      "Tune, wont you come to Limerick.",
      doc.valueOf("/test/many-lines"));
    Assert.assertFalse(
      ("\nThere was a young rustic named Mallory,\n" +
      "who drew but a very small salary.\n" +
      "When he went to the show,\n" +
      "his purse made him go\n" +
      "to a seat in the uppermost gallery.\n" +
      "Tune, wont you come to Limerick.")
      .equals(doc.valueOf("/test/many-lines/text()")));
  }
}

package com.t11e.discovery.datatool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
  locations={"applicationContext-test.xml"})
public class ChangesetExtractorTest
{
  @Autowired
  private ChangesetExtractor extractor;

  @Test
  public void testEmptyChangeset() throws Exception
  {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    extractor.getChangesetForRange(os, null, null);

    final CountingChangesetListener listener = new CountingChangesetListener();
    ChangesetStaxUtil.parseChangeset(
      new ByteArrayInputStream(os.toByteArray()), listener);

    Assert.assertEquals("Should have no set items",
      0, listener.getSetItemCount());
    Assert.assertEquals("Should have no remove items",
      0, listener.getRemoveItemCount());
  }

  @Test
  public void testDateRange()
  {

  }

  @Test
  public void testLargeChangeset()
  {
  }
}

package com.t11e.discovery.datatool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
  locations={"applicationContext-test.xml"})
public class ChangesetExtractorTest
{
  @Resource(name="emptyTable")
  private ChangesetExtractor emptyTableExtractor;
  @Resource(name="dateRange")
  private ChangesetExtractor dateRangeExtractor;

  @Test
  public void testEmptyChangeset() throws Exception
  {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    emptyTableExtractor.getChangesetForRange(os, null, null);

    final CountingChangesetListener listener = new CountingChangesetListener();
    ChangesetStaxUtil.parseChangeset(
      new ByteArrayInputStream(os.toByteArray()), listener);

    Assert.assertEquals("Should have no set items",
      0, listener.getSetItemCount());
    Assert.assertEquals("Should have no remove items",
      0, listener.getRemoveItemCount());
  }

  @Test
  public void testDateRange() throws Exception
  {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final DateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss.S");
    final Date start = format.parse("2010-02-01-00.00.00.0000");
    final Date end = format.parse("2010-02-01-00.00.03.0000");

    dateRangeExtractor.getChangesetForRange(os, start, end);

    final CountingChangesetListener listener = new CountingChangesetListener();
    ChangesetStaxUtil.parseChangeset(
      new ByteArrayInputStream(os.toByteArray()), listener);

    Assert.assertEquals("Should have set items",
      2, listener.getSetItemCount());
    Assert.assertEquals("Should have no remove items",
      0, listener.getRemoveItemCount());
  }

  @Test
  public void testLargeChangeset()
  {
  }
}

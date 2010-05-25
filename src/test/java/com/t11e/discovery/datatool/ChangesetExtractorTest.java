package com.t11e.discovery.datatool;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;
import javax.xml.stream.XMLStreamException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
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
  private Mockery mockery;

  @Before
  public void setup()
  {
    mockery = new Mockery();
  }

  @Test
  public void testEmptyChangeset()
    throws XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations() {{
      never(writer);
    }});
    emptyTableExtractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDateRange()
    throws ParseException, XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations() {{
      never(writer);
      oneOf(writer).setItem(
        "1",
        CollectionsFactory.makeMap(
          "id", "1"));
      oneOf(writer).setItem(
        "2",
        CollectionsFactory.makeMap(
          "id", "2"));
    }});

    final DateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss.S");
    final Date start = format.parse("2010-02-01-00.00.00.0000");
    final Date end = format.parse("2010-02-01-00.00.03.0000");

    dateRangeExtractor.writeChangeset(writer, "delta", start, end);
    mockery.assertIsSatisfied();
  }
}

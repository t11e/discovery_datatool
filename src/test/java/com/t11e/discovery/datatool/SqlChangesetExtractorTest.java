package com.t11e.discovery.datatool;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sql.DataSource;
import javax.xml.stream.XMLStreamException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = {"applicationContext-test.xml"})
public class SqlChangesetExtractorTest
{
  @Autowired
  private DataSource dataSource;
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
    mockery.checking(new Expectations()
    {
      {
        never(writer);
      }
    });

    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from empty_table");
      extractor.setActions(CollectionsFactory.makeList(
        action));
    }
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDateRange()
    throws ParseException, XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.makeMap(
            "id", "1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.makeMap(
            "id", "2"));
      }
    });
    final DateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss.S");
    final Date start = format.parse("2010-02-01-00.00.00.0000");
    final Date end = format.parse("2010-02-01-00.00.03.0000");
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery(
        "select id " +
          "from date_range_test " +
          "where last_updated >= :start and last_updated < :end");
      extractor.setActions(CollectionsFactory.makeList(
        action));
    }
    extractor.writeChangeset(writer, "delta", start, end);
    mockery.assertIsSatisfied();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testStringColumns()
    throws ParseException, XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.makeMap(
            "id", "1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.makeMap(
            "id", "2",
            "col_fixed", "",
            "col_string", "",
            "col_clob", ""));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.makeMap(
            "id", "3",
            "col_fixed", "a",
            "col_string", "b",
            "col_clob", "c"));
        oneOf(writer).setItem(
          "4",
          CollectionsFactory.makeMap(
            "id", "4",
            "col_fixed", "a",
            "col_string", "b",
            "col_clob", "c"));
      }
    });
    testExtractor(writer, "string_column_test");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNumericColumns()
    throws ParseException, XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.makeMap(
            "id", "1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.makeMap(
            "id", "2",
            "col_int", "0",
            "col_double", "0.0"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.makeMap(
            "id", "3",
            "col_int", "12",
            "col_double", "34.56"));
      }
    });
    testExtractor(writer, "numeric_column_test");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDateTimeColumns()
    throws ParseException, XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.makeMap(
            "id", "1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.makeMap(
            "id", "2",
            "col_date", "2010-01-01",
            "col_time", "00:00:00",
            "col_datetime", "2010-01-01T00:00:00.000"));
      }
    });
    testExtractor(writer, "datetime_column_test");
  }

  private void testExtractor(
    final ChangesetWriter writer,
    final String tableName)
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from " + tableName);
      extractor.setActions(CollectionsFactory.makeList(
        action));
    }
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }
}

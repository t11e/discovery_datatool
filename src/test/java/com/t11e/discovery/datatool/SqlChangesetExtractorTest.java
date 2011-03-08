package com.t11e.discovery.datatool;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

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
          CollectionsFactory.<String, String> makeMap(
            "id", "1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, String> makeMap(
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
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    extractor.writeChangeset(writer, "delta", start, end);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testStringColumns()
    throws XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, String> makeMap(
            "id", "1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, String> makeMap(
            "id", "2",
            "col_fixed", "",
            "col_string", "",
            "col_clob", ""));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, String> makeMap(
            "id", "3",
            "col_fixed", "a",
            "col_string", "b",
            "col_clob", "c"));
        oneOf(writer).setItem(
          "4",
          CollectionsFactory.<String, String> makeMap(
            "id", "4",
            "col_fixed", "a",
            "col_string", "b",
            "col_clob", "c"));
      }
    });
    testExtractor(writer, "string_column_test");
  }

  @Test
  public void testNumericColumns()
    throws XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, String> makeMap(
            "id", "1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, String> makeMap(
            "id", "2",
            "col_int", "0",
            "col_double", "0.0"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, String> makeMap(
            "id", "3",
            "col_int", "12",
            "col_double", "34.56"));
      }
    });
    testExtractor(writer, "numeric_column_test");
  }

  @Test
  public void testDateTimeColumns()
    throws XMLStreamException
  {
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, String> makeMap(
            "id", "1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, String> makeMap(
            "id", "2",
            "col_date", "2010-01-01",
            "col_time", "00:00:00",
            "col_datetime", "2010-01-01T00:00:00.000"));
      }
    });
    testExtractor(writer, "datetime_column_test");
  }

  @Test
  public void testSubQueryDelimitedProperty()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.DELIMITED,
        "select name from subquery_joined_test where parent_id=:id", "fish", null, ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fish", "redfish,bluefish"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fish", "onefish,twofish"));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryArrayProperty()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.ARRAY,
        "select name from subquery_joined_test where parent_id=:id", "fish", null, ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fish", Arrays.asList("redfish", "bluefish")));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fish", Arrays.asList("onefish", "twofish")));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryNonDelimitedProperty()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.ARRAY,
        "select name from subquery_joined_test where parent_id=:id", "fish", null, ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fish", Arrays.asList("redfish", "bluefish")));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fish", Arrays.asList("onefish", "twofish")));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryDelimitedPropertyPrefix()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.DELIMITED,
        "select name from subquery_joined_test where parent_id=:id", null, "fish_", ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fish_name", "redfish,bluefish"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fish_name", "onefish,twofish"));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryDelimitedPropertyPrefixMultiColumns()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.DELIMITED,
        "select parent_id as id, name from subquery_joined_test where parent_id=:id", null, "fish_", ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fish_name", "redfish,bluefish",
            "fish_id", "1,1"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fish_name", "onefish,twofish",
            "fish_id", "3,3"));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSubQueryArrayPropertyMultiColumns()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.ARRAY,
        "select parent_id as id, name from subquery_joined_test where parent_id=:id", "fish", null, ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fish", Arrays.asList(
              CollectionsFactory.<String, Object> makeMap("id", "1", "name", "redfish"),
              CollectionsFactory.<String, Object> makeMap("id", "1", "name", "bluefish"))));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fish", Arrays.asList(
              CollectionsFactory.<String, Object> makeMap("id", "3", "name", "onefish"),
              CollectionsFactory.<String, Object> makeMap("id", "3", "name", "twofish"))));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryDelimitedPropertyMultiColumns()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.DELIMITED,
        "select parent_id as id, name from subquery_joined_test where parent_id=:id", "fish", null, ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fish", CollectionsFactory.<String, Object> makeMap("id", "1,1", "name", "redfish,bluefish")));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fish", CollectionsFactory.<String, Object> makeMap("id", "3,3", "name", "onefish,twofish")));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryNonDelimitedNoProperty()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.ARRAY,
        "select name as fishname from subquery_joined_test where parent_id=:id", null, null, ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fishname", Arrays.asList("redfish", "bluefish")));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fishname", Arrays.asList("onefish", "twofish")));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryDelimitedNoProperty()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.DELIMITED,
        "select name as fishname from subquery_joined_test where parent_id=:id", null, null, ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fishname", "redfish,bluefish"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fishname", "onefish,twofish"));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryDelimitedNoPropertyMultiColumns()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.DELIMITED,
        "select parent_id as fishid, name as fishname from subquery_joined_test where parent_id=:id", null, null, ",",
        null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fishid", "1,1",
            "fishname", "redfish,bluefish"));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fishid", "3,3",
            "fishname", "onefish,twofish"));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryArrayNoProperty()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.ARRAY,
        "select name as fishname from subquery_joined_test where parent_id=:id", null, null, ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fishname", Arrays.asList("redfish", "bluefish")));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fishname", Arrays.asList("onefish", "twofish")));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }

  @Test
  public void testSubQueryArrayNoPropertyMultiColumns()
    throws XMLStreamException
  {
    final SqlChangesetExtractor extractor = new SqlChangesetExtractor();
    extractor.setDataSource(dataSource);
    {
      final SqlAction action = new SqlAction();
      action.setAction("create");
      action.setIdColumn("id");
      action.setQuery("select * from subquery_test");
      action.setSubqueries(Arrays.asList(new SubQuery(SubQuery.Type.ARRAY,
        "select parent_id as fishfoo, name as fishname from subquery_joined_test where parent_id=:id", null, null,
        ",", null)));
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    final ChangesetWriter writer = mockery.mock(ChangesetWriter.class);
    mockery.checking(new Expectations()
    {
      {
        never(writer);
        oneOf(writer).setItem(
          "1",
          CollectionsFactory.<String, Object> makeMap(
            "id", "1",
            "fishfoo", Arrays.asList("1", "1"),
            "fishname", Arrays.asList("redfish", "bluefish")));
        oneOf(writer).setItem(
          "2",
          CollectionsFactory.<String, Object> makeMap(
            "id", "2"));
        oneOf(writer).setItem(
          "3",
          CollectionsFactory.<String, Object> makeMap(
            "id", "3",
            "fishfoo", Arrays.asList("3", "3"),
            "fishname", Arrays.asList("onefish", "twofish")));
      }
    });
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
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
      extractor.setFilteredActions(CollectionsFactory.makeList(action));
    }
    extractor.writeChangeset(writer, "snapshot", null, null);
    mockery.assertIsSatisfied();
  }
}

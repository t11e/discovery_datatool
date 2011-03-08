package com.t11e.discovery.datatool;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

public class CreateActionRowCallbackHandler
  implements RowCallbackHandler
{
  private static final Logger logger = Logger.getLogger(CreateActionRowCallbackHandler.class.getName());
  private final ChangesetWriter writer;
  private final String idColumn;
  private final String idPrefix;
  private final String idSuffix;
  private final List<SubQuery> subqueries;
  private final NamedParameterJdbcOperations jdbcTemplate;
  private final ResultSetConvertor resultSetConvertor;
  private final List<ResultSetConvertor> subqueryConvertors;
  private final boolean shouldRecordTimings;
  private long totalTime;
  private int numSubQueries;

  public CreateActionRowCallbackHandler(
    final NamedParameterJdbcOperations jdbcTemplate,
    final ChangesetWriter writer,
    final String idColumn,
    final String idPrefix,
    final String idSuffix,
    final boolean lowerCaseColumnNames,
    final Set<String> jsonColumns,
    final List<SubQuery> subqueries,
    final boolean shouldRecordTimings)
  {
    this.jdbcTemplate = jdbcTemplate;
    this.writer = writer;
    this.idColumn = idColumn;
    this.idPrefix = idPrefix;
    this.idSuffix = idSuffix;
    this.shouldRecordTimings = shouldRecordTimings;
    this.subqueries = subqueries != null ? subqueries : Collections.<SubQuery> emptyList();
    resultSetConvertor = new ResultSetConvertor(lowerCaseColumnNames, jsonColumns);
    if (this.subqueries.isEmpty())
    {
      subqueryConvertors = Collections.emptyList();
    }
    else
    {
      subqueryConvertors = new ArrayList<ResultSetConvertor>(this.subqueries.size());
      for (int i = 0; i < this.subqueries.size(); ++i)
      {
        subqueryConvertors.add(new ResultSetConvertor(lowerCaseColumnNames, Collections.<String> emptySet()));
      }
    }
  }

  @Override
  public void processRow(final ResultSet rs)
    throws SQLException
  {
    final String id = getId(rs);
    final Map<String, Object> properties = resultSetConvertor.getRowAsMap(rs);
    final CaseInsensitveParameterSource subqueryParams = new CaseInsensitveParameterSource(properties);
    for (int i = 0; i < subqueries.size(); ++i)
    {
      final SubQuery subquery = subqueries.get(i);
      final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
      {
        final StopWatch watch = StopWatchHelper.startTimer(shouldRecordTimings);
        try
        {
          jdbcTemplate.query(subquery.getQuery(), subqueryParams,
            new SubqueryRowCallbackHandler(values, subqueryConvertors.get(i)));
        }
        catch (final InvalidDataAccessApiUsageException e)
        {
          throw new RuntimeException(e.getMessage() + " (available keys: " + properties.keySet() + ")", e);
        }
        recordQueryTime(watch);
      }
      if (!values.isEmpty())
      {
        if (StringUtils.isNotBlank(subquery.getDiscriminator()))
        {
          final Map<String, Object> groupedbyDiscriminator = new LinkedHashMap<String, Object>();
          for (final Map<String, Object> row : values)
          {
            final String discriminatorValue = (String) row.remove(subquery.getDiscriminator());
            if (discriminatorValue != null)
            {
              groupedbyDiscriminator.put(discriminatorValue, row);
            }
          }
          if (!groupedbyDiscriminator.isEmpty())
          {
            properties.put(subquery.getProperty(), groupedbyDiscriminator);
          }
        }
        else if (StringUtils.isBlank(subquery.getProperty()))
        {
          final String propertyPrefix = StringUtils.isNotBlank(subquery.getPropertyPrefix()) ? subquery.getPropertyPrefix() : "";
          if (values.size() == 1)

          {
            final Map<String, Object> row = values.get(0);
            for (final Entry<String, Object> entry : row.entrySet())
            {
              properties.put(propertyPrefix + entry.getKey(), entry.getValue());
            }
          }
          else if (values.size() > 1)
          {
            final Map<String, List<Object>> flattened = new LinkedHashMap<String, List<Object>>();
            for (final Map<String, Object> row : values)
            {
              for (final Entry<String, Object> columnValue : row.entrySet())
              {
                if (!flattened.containsKey(columnValue.getKey()))
                {
                  flattened.put(columnValue.getKey(), new ArrayList<Object>(values.size()));
                }
                flattened.get(columnValue.getKey()).add(columnValue.getValue());
              }
            }
            for (final Entry<String, List<Object>> flattenedValues : flattened.entrySet())
            {
              properties.put(propertyPrefix + flattenedValues.getKey(),
                SubQuery.Type.DELIMITED.equals(subquery.getType())
                  ? StringUtils.join(flattenedValues.getValue(), subquery.getDelimiter())
                  : flattenedValues.getValue());
            }
          }
        }
        // use single property
        else
        {
          final boolean multiColumn = values.size() > 0 && values.get(0).size() != 1;
          if (values.size() == 1)
          {
            final Map<String, Object> row = values.get(0);
            if (multiColumn)
            {
              final Map<String, List<Object>> flattened = new LinkedHashMap<String, List<Object>>();
              for (final Entry<String, Object> columnValue : row.entrySet())
              {
                if (!flattened.containsKey(columnValue.getKey()))
                {
                  flattened.put(columnValue.getKey(), new ArrayList<Object>(values.size()));
                }
                flattened.get(columnValue.getKey()).add(columnValue.getValue());
              }
              final Object value;
              if (SubQuery.Type.DELIMITED.equals(subquery.getType()))
              {
                final Map<String, String> combined = new LinkedHashMap<String, String>();
                value = combined;
                for (final Entry<String, List<Object>> flattenedValues : flattened.entrySet())
                {
                  combined.put(flattenedValues.getKey(), StringUtils.join(flattenedValues.getValue(), subquery.getDelimiter()));
                }
              }
              else
              {
                final Map<String, Object> remapped = new LinkedHashMap<String, Object>(flattened.size());
                for (final Entry<String, List<Object>> entry : flattened.entrySet())
                {
                  final List<Object> entryValue = entry.getValue();
                  remapped.put(entry.getKey(), entryValue.size() == 1 ? entryValue.get(0) : entryValue);
                }
                value = remapped;
              }
              properties.put(subquery.getProperty(), value);
            }
            else
            {
              properties.put(subquery.getProperty(), row.values().iterator().next());
            }
          }
          else if (values.size() > 1)
          {
            if (multiColumn)
            {
              final Object value;
              if (SubQuery.Type.DELIMITED.equals(subquery.getType()))
              {
                final Map<String, List<Object>> flattened = new LinkedHashMap<String, List<Object>>();
                for (final Map<String, Object> row : values)
                {
                  for (final Entry<String, Object> columnValue : row.entrySet())
                  {
                    if (!flattened.containsKey(columnValue.getKey()))
                    {
                      flattened.put(columnValue.getKey(), new ArrayList<Object>(values.size()));
                    }
                    flattened.get(columnValue.getKey()).add(columnValue.getValue());
                  }
                }
                final Map<String, String> combined = new LinkedHashMap<String, String>();
                value = combined;
                for (final Entry<String, List<Object>> flattenedValues : flattened.entrySet())
                {
                  combined.put(flattenedValues.getKey(), StringUtils.join(flattenedValues.getValue(), subquery.getDelimiter()));
                }
              }
              else
              {
                value = values;
              }
              properties.put(subquery.getProperty(), value);
            }
            else
            {
              final List<Object> flattened = new ArrayList<Object>(values.size());
              for (final Map<String, Object> row : values)
              {
                flattened.add(row.values().iterator().next());
              }
              properties.put(subquery.getProperty(),
                SubQuery.Type.DELIMITED.equals(subquery.getType())
                  ? StringUtils.join(flattened, subquery.getDelimiter())
                  : flattened);
            }
          }
        }
      }
    }
    try
    {
      writer.setItem(id, properties);
    }
    catch (final XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void recordQueryTime(final StopWatch watch)
  {
    if (shouldRecordTimings && watch != null)
    {
      watch.stop();
      totalTime += watch.getTime();
      ++numSubQueries;
      if (logger.isLoggable(Level.FINEST))
      {
        logger.finest("Subquery took [" + watch.getTime() + "]ms [" + watch + "]");
      }
    }
  }

  private String getId(final ResultSet rs)
    throws SQLException
  {
    final String id;
    {
      final StringBuilder builder = new StringBuilder();
      if (StringUtils.isNotBlank(idPrefix))
      {
        builder.append(idPrefix);
      }
      builder.append(rs.getString(idColumn));
      if (StringUtils.isNotBlank(idSuffix))
      {
        builder.append(idSuffix);
      }
      id = builder.toString();
    }
    return id;
  }

  public long getTotalTime()
  {
    return totalTime;
  }

  public int getNumSubQueries()
  {
    return numSubQueries;
  }

  private static final class SubqueryRowCallbackHandler
    implements RowCallbackHandler
  {
    private final List<Map<String, Object>> values;
    private final ResultSetConvertor convertor;

    private SubqueryRowCallbackHandler(final List<Map<String, Object>> values, final ResultSetConvertor convertor)
    {
      this.values = values;
      this.convertor = convertor;
    }

    @Override
    public void processRow(final ResultSet rs)
      throws SQLException
    {
      values.add(convertor.getRowAsMap(rs));
    }
  }
}

package com.t11e.discovery.datatool;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SqlChangesetExtractor implements ChangesetExtractor
{
  private DataSource dataSource;
  private List<SqlAction> sqlActions;
  // Locally created
  private NamedParameterJdbcTemplate jdbcTemplate;

  @PostConstruct
  public void initialize()
  {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  public void writeChangeset(
    final ChangesetWriter writer,
    final String changesetType,
    final Date start,
    final Date end)
  {
    for (final SqlAction sqlAction : sqlActions)
    {
      final Set<String> filters = sqlAction.getFilter();
      if (filters.contains("all") || filters.contains(changesetType))
      {
        process(writer, sqlAction, changesetType, start, end);
      }
    }
  }

  private void process(
    final ChangesetWriter writer,
    final SqlAction sqlAction,
    final String kind,
    final Date start,
    final Date end)
  {
    final MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("start", start);
    params.addValue("end", end);
    params.addValue("kind", kind);
    final RowCallbackHandler callbackHandler;
    if ("create".equals(sqlAction.getAction()))
    {
      callbackHandler =
        new CreateActionRowCallbackHandler(
          writer,
          sqlAction.getIdColumn(),
          sqlAction.getIdPrefix(),
          sqlAction.getIdSuffix(),
          sqlAction.isUseLowerCaseColumnNames(),
          sqlAction.getJsonColumnNames());
    }
    else if ("delete".equals(sqlAction.getAction()))
    {
      callbackHandler =
        new DeleteActionRowCallbackHandler(writer, sqlAction.getIdColumn());
    }
    else
    {
      throw new RuntimeException("Unknown action: " + sqlAction.getAction());
    }
    jdbcTemplate.query(sqlAction.getQuery(), params, callbackHandler);
  }

  @Required
  public void setDataSource(final DataSource dataSource)
  {
    this.dataSource = dataSource;
  }
  @Required
  public void setActions(final List<SqlAction> actions)
  {
    this.sqlActions = actions;
  }
}

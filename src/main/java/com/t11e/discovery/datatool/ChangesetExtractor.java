package com.t11e.discovery.datatool;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ChangesetExtractor
{
  @Autowired
  private DataSource dataSource;
  private List<SqlTemplate> sqlTemplates;
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
    for (final SqlTemplate sqlTemplate : sqlTemplates)
    {
      if (sqlTemplate.getFilter().contains(changesetType))
      {
        process(writer, sqlTemplate, changesetType, start, end);
      }
    }
  }

  private void process(
    final ChangesetWriter writer,
    final SqlTemplate sqlTemplate,
    final String kind,
    final Date start,
    final Date end)
  {
    final MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("start", start);
    params.addValue("end", end);
    params.addValue("kind", kind);
    final RowCallbackHandler callbackHandler;
    if ("create".equals(sqlTemplate.getAction()))
    {
      callbackHandler =
        new CreateActionRowCallbackHandler(
          writer,
          sqlTemplate.getIdColumn(),
          sqlTemplate.getIdPrefix(),
          sqlTemplate.getIdSuffix(),
          sqlTemplate.isUseLowerCaseColumnNames(),
          sqlTemplate.getJsonColumnNames());
    }
    else if ("delete".equals(sqlTemplate.getAction()))
    {
      callbackHandler =
        new DeleteActionRowCallbackHandler(writer, sqlTemplate.getIdColumn());
    }
    else
    {
      throw new RuntimeException("Unknown action: " + sqlTemplate.getAction());
    }
    jdbcTemplate.query(sqlTemplate.getQuery(), params, callbackHandler);
  }

  @Required
  public void setSqlTemplates(final List<SqlTemplate> selectTemplates)
  {
    this.sqlTemplates = selectTemplates;
  }
}

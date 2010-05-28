package com.t11e.discovery.datatool;

import java.util.Date;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SqlChangesetProfileService
  implements ChangesetProfileService
{
  private String tableName;
  private String nameColumn;
  private String lastRunColumn;
  private NamedParameterJdbcTemplate jdbcTemplate;

  public Date[] getChangesetProfileDateRange(final String profile)
  {
    final Date start;
    {
      final String sql =
        "select " + lastRunColumn + " " +
        "from " + tableName + " " +
        "where " + nameColumn + " = :name";
      final MapSqlParameterSource params = new MapSqlParameterSource();
      params.addValue("name", profile);
      start = jdbcTemplate.queryForObject(sql, params, Date.class);
    }
    final Date end;
    {
      final String sql = "select CURRENT_TIMESTAMP from " + tableName;
      final MapSqlParameterSource params = new MapSqlParameterSource();
      end = jdbcTemplate.queryForObject(sql, params, Date.class);
    }
    return new Date[] {start, end};
  }

  public void saveChangesetProfileLastRun(
    final String profile,
    final Date lastRun)
  {
    final String sql =
      "update " + tableName + " " +
      "set " + lastRunColumn + " = :lastRun " +
      "where " + nameColumn + " = :name";
    final MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("name", profile);
    params.addValue("lastRun", lastRun);
    jdbcTemplate.update(sql, params);
  }

  @Required
  public void setTableName(final String tableName)
  {
    this.tableName = tableName;
  }
  @Required
  public void setNameColumn(final String nameColumn)
  {
    this.nameColumn = nameColumn;
  }
  @Required
  public void setLastRunColumn(final String lastRunColumn)
  {
    this.lastRunColumn = lastRunColumn;
  }
  @Required
  public void setDataSource(final DataSource dataSource)
  {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }
}

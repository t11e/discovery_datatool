package com.t11e.discovery.datatool;

import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SqlChangesetProfileService
  implements ChangesetProfileService
{
  private String retrieveSql;
  private String retrieveStartColumn;
  private String retrieveEndColumn;
  private String updateSql;
  private NamedParameterJdbcTemplate jdbcTemplate;

  public Date[] getChangesetProfileDateRange(final String profile)
  {
    final Date start;
    final Date end;
    {
      final MapSqlParameterSource params = new MapSqlParameterSource();
      params.addValue("name", profile);
      try
      {

        final Map<String, Object> data = jdbcTemplate.queryForMap(retrieveSql, params);
        start = (Date) data.get(retrieveStartColumn);
        end = (Date) data.get(retrieveEndColumn);
        if (end == null)
        {
          throw new IllegalStateException("End date cannot be null");
        }
      }
      catch (final EmptyResultDataAccessException e)
      {
        throw new NoSuchProfileException(profile);
      }
    }
    return new Date[] {start, end};
  }

  public void saveChangesetProfileLastRun(
    final String profile,
    final Date lastRun)
  {
    final MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("name", profile);
    params.addValue("lastRun", lastRun);
    final int updated = jdbcTemplate.update(updateSql, params);
    if (updated != 1)
    {
      throw new RuntimeException("Unexpected update count when saving changeset profile "
        + profile + " " + updated);
    }
  }

  @Required
  public void setDataSource(final DataSource dataSource)
  {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }
  @Required
  public void setRetrieveSql(final String retrieveSql)
  {
    this.retrieveSql = retrieveSql;
  }
  @Required
  public void setRetrieveStartColumn(final String retrieveStartColumn)
  {
    this.retrieveStartColumn = retrieveStartColumn;
  }
  @Required
  public void setRetrieveEndColumn(final String retrieveEndColumn)
  {
    this.retrieveEndColumn = retrieveEndColumn;
  }
  @Required
  public void setUpdateSql(final String updateSql)
  {
    this.updateSql = updateSql;
  }
}

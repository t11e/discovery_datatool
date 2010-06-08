package com.t11e.discovery.datatool;

import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SqlChangesetProfileService
  implements ChangesetProfileService
{
  private String createSql;
  private String retrieveSql;
  private String retrieveStartColumn;
  private String retrieveEndColumn;
  private String updateSql;
  private NamedParameterJdbcTemplate jdbcTemplate;

  public Date[] getChangesetProfileDateRange(final String profile)
  {
    return getChangesetProfileDateRange(profile, true);
  }

  private Date[] getChangesetProfileDateRange(final String profile, final boolean shouldCreateProfile)
  {
    Date[] startEnd;
    {
      final MapSqlParameterSource params = new MapSqlParameterSource();
      params.addValue("name", profile);
      try
      {
        final Map<String, Object> data = jdbcTemplate.queryForMap(retrieveSql, params);
        startEnd = new Date[] {
            (Date) data.get(retrieveStartColumn),
            (Date) data.get(retrieveEndColumn)
        };
        if (startEnd[1] == null)
        {
          throw new IllegalStateException("End date cannot be null");
        }
      }
      catch (final EmptyResultDataAccessException e)
      {
        if (shouldCreateProfile && StringUtils.isNotBlank(createSql))
        {
          createProfile(profile);
          startEnd = getChangesetProfileDateRange(profile, false);
        }
        else
        {
          throw new NoSuchProfileException(profile);
        }
      }
    }
    return startEnd;
  }

  @SuppressWarnings("unchecked")
  private void createProfile(final String profile)
  {
    jdbcTemplate.update(createSql, CollectionsFactory.makeMap("name", profile));
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
  /** Optional. Setting this enables profile auto-creation. */
  public void setCreateSql(final String createSql)
  {
    this.createSql = createSql;
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

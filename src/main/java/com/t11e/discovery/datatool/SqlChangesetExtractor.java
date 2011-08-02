package com.t11e.discovery.datatool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.JdbcUtils;

public class SqlChangesetExtractor
  implements ChangesetExtractor
{
  private static final Logger logger = Logger.getLogger(SqlChangesetExtractor.class.getName());
  private Collection<SqlAction> filteredActions = Collections.emptyList();
  private Collection<SqlAction> completeActions = Collections.emptyList();
  private Collection<SqlAction> incrementalActions = Collections.emptyList();
  private String completeActionType;
  private DataSource dataSource;

  @Override
  public void writeChangeset(
    final ChangesetWriter writer,
    final String changesetType,
    final Date start,
    final Date end)
  {
    Connection conn = null;
    try
    {
      conn = dataSource.getConnection();
      final NamedParameterJdbcOperations jdbcTemplate =
          new NamedParameterJdbcTemplate(new SingleConnectionDataSource(conn, true));
      for (final SqlAction action : filteredActions)
      {
        final Set<String> filters = action.getFilter();
        if (filters.contains("any") || filters.contains(changesetType))
        {
          process(jdbcTemplate, writer, action, changesetType, start, end);
        }
      }
      if (start == null)
      {
        for (final SqlAction action : completeActions)
        {
          process(jdbcTemplate, writer, action, changesetType, start, end);
        }
      }
      else
      {
        for (final SqlAction action : incrementalActions)
        {
          process(jdbcTemplate, writer, action, changesetType, start, end);
        }
      }
    }
    catch (final SQLException e)
    {
      throw new RuntimeException(e);
    }
    finally
    {
      JdbcUtils.closeConnection(conn);
    }
  }

  @Override
  public String determineType(final Date start)
  {
    String result = start == null ? "snapshot" : "delta";
    if (!completeActions.isEmpty())
    {
      if (start == null)
      {
        result = completeActionType;
      }
      else if (incrementalActions.isEmpty())
      {
        result = completeActionType;
      }
    }
    return result;
  }

  private void process(
    final NamedParameterJdbcOperations jdbcTemplate,
    final ChangesetWriter writer,
    final SqlAction sqlAction,
    final String kind,
    final Date start,
    final Date end)
  {
    final boolean logTiming = logger.isLoggable(Level.FINEST);
    final SqlParameterSource params = new CaseInsensitveParameterSource()
      .addValue("start", start)
      .addValue("end", end)
      .addValue("kind", kind);
    final CompletionAwareRowCallbackHandler callbackHandler;
    if ("create".equals(sqlAction.getAction()))
    {
      callbackHandler =
          new CreateActionRowCallbackHandler(
            jdbcTemplate,
            writer,
            sqlAction.getIdColumn(),
            sqlAction.getIdPrefix(),
            sqlAction.getIdSuffix(),
            sqlAction.isUseLowerCaseColumnNames(),
            sqlAction.getJsonColumnNames(),
            sqlAction.getMergeColumns(),
            sqlAction.getSubqueries(),
            logTiming);
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
    {
      final StopWatch watch = StopWatchHelper.startTimer(logTiming);
      jdbcTemplate.query(sqlAction.getQuery(), params, callbackHandler);
      callbackHandler.flushItem();
      logQueryTimes(logTiming, watch, callbackHandler);
    }
  }

  private void logQueryTimes(final boolean shouldLog, final StopWatch watch, final RowCallbackHandler callbackHandler)
  {
    if (shouldLog && watch != null)
    {
      watch.stop();
      logger.fine("Query and subqueries took " + watch.getTime() + "ms [" + watch + "] total");
      if (callbackHandler instanceof CreateActionRowCallbackHandler)
      {
        final CreateActionRowCallbackHandler carch = (CreateActionRowCallbackHandler) callbackHandler;
        logger.fine("Subquery total time " + carch.getTotalTime() + "ms for " + carch.getNumSubQueries()
          + " queries ("
          + (carch.getTotalTime() * 1.0 / carch.getNumSubQueries()) + " ms avg) - main query took "
          + (watch.getTime() - carch.getTotalTime()));
      }
    }
  }

  @Required
  public void setDataSource(final DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  @Required
  public void setFilteredActions(final Collection<SqlAction> actions)
  {
    filteredActions = actions;
  }

  @Required
  public void setCompleteActions(final Collection<SqlAction> completeActions)
  {
    this.completeActions = completeActions;
    if (!completeActions.isEmpty())
    {
      final Set<String> filter = completeActions.iterator().next().getFilter();
      if (!filter.isEmpty())
      {
        completeActionType = filter.iterator().next();
      }
    }
    else
    {
      completeActionType = "";
    }
  }

  @Required
  public void setIncrementalActions(final Collection<SqlAction> incrementalActions)
  {
    this.incrementalActions = incrementalActions;
  }
}

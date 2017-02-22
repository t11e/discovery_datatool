package com.t11e.discovery.datatool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.JdbcUtils;

public class SqlChangesetExtractor
  implements ChangesetExtractor, Validatable
{
  private static final Logger logger = Logger.getLogger(SqlChangesetExtractor.class.getName());
  private static final Logger progressLogger = Logger.getLogger(SqlChangesetExtractor.class.getName() + ".PROGRESS");
  private static final Logger sqlLogger = Logger.getLogger(SqlChangesetExtractor.class.getName() + ".SQL");
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
    @SuppressWarnings("unchecked")
    final Collection<SqlAction> realizedFilteredActions = CollectionUtils.select(filteredActions, new Predicate()
    {
      @Override
      public boolean evaluate(final Object arg)
      {
        final SqlAction action = (SqlAction) arg;
        final Set<String> filters = action.getFilter();
        return filters.contains("any") || filters.contains(changesetType);
      }
    });
    final ProgressLogger progress = new ProgressLoggerSimpleJavaUtil(progressLogger, Level.FINE,
      TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));
    progress
      .setEstimatedWork(
        realizedFilteredActions.size() + (start == null ? completeActions.size() : incrementalActions.size()),
        "*-item configuration element",
        "*-item configuration elements");
    progress.begin("configuration elements");
    Connection conn = null;
    try
    {
      conn = dataSource.getConnection();
      final NamedParameterJdbcOperations jdbcTemplate = LoggingNamedParameterJdbcTemplate
        .create(new SingleConnectionDataSource(conn, true), sqlLogger, Level.FINEST);
      for (final SqlAction action : realizedFilteredActions)
      {
          process(jdbcTemplate, writer, action, changesetType, start, end);
        progress.worked(1);
      }
      if (start == null)
      {
        for (final SqlAction action : completeActions)
        {
          process(jdbcTemplate, writer, action, changesetType, start, end);
          progress.worked(1);
        }
      }
      else
      {
        for (final SqlAction action : incrementalActions)
        {
          process(jdbcTemplate, writer, action, changesetType, start, end);
          progress.worked(1);
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
      progress.done();
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

  private static void process(
    final NamedParameterJdbcOperations jdbcTemplate,
    final ChangesetWriter writer,
    final SqlAction sqlAction,
    final String kind,
    final Date start,
    final Date end)
  {
    final ProgressLogger progress = new ProgressLoggerSimpleJavaUtil(progressLogger, Level.FINE,
      TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS))
      .setUnits("item", "items");

    final Level logTimingLevel = Level.FINEST;
    final boolean logTiming = progressLogger.isLoggable(logTimingLevel);
    final SqlParameterSource params = new CaseInsensitveParameterSource()
      .addValue("start", start)
      .addValue("end", end)
      .addValue("kind", kind);
    final CompletionAwareRowCallbackHandler callbackHandler;
    if ("create".equals(sqlAction.getAction()) || "add".equals(sqlAction.getAction()))
    {
      callbackHandler =
          new CreateActionRowCallbackHandler(
            jdbcTemplate,
            writer,
            "create".equals(sqlAction.getAction()) ? ChangesetElement.SET_ITEM : ChangesetElement.ADD_TO_ITEM,
            sqlAction.getIdColumn(),
            sqlAction.getProviderColumn(),
            sqlAction.getKindColumn(),
            sqlAction.getPropertyCase(),
            sqlAction.getScopedJsonColumnsSet(),
            sqlAction.getUnscopedJsonColumnsSet(),
            sqlAction.getMergeColumns(),
            sqlAction.getSubqueries(),
            logTiming,
            progress);
    }
    else if ("delete".equals(sqlAction.getAction()))
    {
      callbackHandler =
          new DeleteActionRowCallbackHandler(
            writer,
            sqlAction.getIdColumn(),
            sqlAction.getProviderColumn(),
            sqlAction.getKindColumn(),
            progress);
    }
    else
    {
      throw new RuntimeException("Unknown action: " + sqlAction.getAction());
    }
    {
      final StopWatch watch = StopWatchHelper.startTimer(logTiming);
      progress.begin(StringUtils.rightPad(sqlAction.getChangesetElementType(), "add-to-item".length()) + " " +
        abbreviateSql(sqlAction));
      try
      {
        jdbcTemplate.query(sqlAction.getQuery(), params, callbackHandler);
        callbackHandler.flushItem();
      }
      finally
      {
        progress.done();
      }
      if (logTiming)
      {
        logQueryTimes(progressLogger, logTimingLevel, watch, callbackHandler);
      }
    }
  }

  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private static String abbreviateSql(final SqlAction sqlAction)
  {
    final String sql = WHITESPACE.matcher(sqlAction.getQuery()).replaceAll(" ");
    return StringUtils.abbreviate(sql, 40);
  }

  private static void logQueryTimes(final Logger logger, final Level level, final StopWatch watch,
    final RowCallbackHandler callbackHandler)
  {
    if (watch != null)
    {
      watch.stop();
      logger.log(level, "Query and subqueries took " + watch.getTime() + "ms [" + watch + "] total");
      if (callbackHandler instanceof CreateActionRowCallbackHandler)
      {
        final CreateActionRowCallbackHandler carch = (CreateActionRowCallbackHandler) callbackHandler;
        logger.log(level, "Subquery total time " + carch.getTotalTime() + "ms for " + carch.getNumSubQueries()
          + " queries ("
          + (carch.getTotalTime() * 1.0 / carch.getNumSubQueries()) + " ms avg) - main query took "
          + (watch.getTime() - carch.getTotalTime()));
      }
    }
  }

  @Override
  public Collection<String> checkValid(final String context)
  {
    final Collection<String> result = new ArrayList<String>();
    Connection conn = null;
    try
    {
      conn = dataSource.getConnection();
      final JdbcTemplate jdbcTemplate =
          new JdbcTemplate(new SingleConnectionDataSource(conn, true));
      jdbcTemplate.setMaxRows(1);
      jdbcTemplate.setFetchSize(1);
      for (final SqlAction action : filteredActions)
      {
        validateAction(result, context, jdbcTemplate, action);
      }
      for (final SqlAction action : completeActions)
      {
        validateAction(result, context, jdbcTemplate, action);
      }
      for (final SqlAction action : incrementalActions)
      {
        validateAction(result, context, jdbcTemplate, action);
      }
    }
    catch (final SQLException e)
    {
      throw new RuntimeException("Unable to obtain database connection.", e);
    }
    finally
    {
      JdbcUtils.closeConnection(conn);
    }

    return result;
  }

  private static void validateAction(final Collection<String> result, final String context,
    final JdbcOperations jdbcTemplate, final SqlAction action)
  {
    {
      final SqlParameterSource params = new CaseInsensitveParameterSource()
        .addValue("kind", "delta")
        .addValue("start", new Date())
        .addValue("end", new Date());
      checkValidSql(result, context, jdbcTemplate, action.getQuery(), params);
    }
    for (final SubQuery subquery : action.getSubqueries())
    {
      checkValidSql(result, context, jdbcTemplate, subquery.getQuery(),
        new NullDefaultCaseInsensitiveParameterSource());
    }
  }

  private static void checkValidSql(final Collection<String> result, final String context,
    final JdbcOperations jdbcTemplate, final String sql, final SqlParameterSource params)
  {
    final long start = System.nanoTime();
    try
    {
      final ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
      final Object[] values = NamedParameterUtils.buildValueArray(parsedSql, params, null);
      jdbcTemplate.query(NamedParameterUtils.substituteNamedParameters(sql, params), values,
        new ResultSetExtractor<Void>()
      {
        @Override
        public Void extractData(final ResultSet rs)
          throws SQLException, DataAccessException
        {
          return null;
        }
      });
    }
    catch (final DataAccessException e)
    {
      result.add(context + ": problem with \n" + sql + "\n" + e.getLocalizedMessage());
    }
    final long end = System.nanoTime();
    final long elapsedNs = end - start;
    logger.finest("Checking validity took " + (elapsedNs / 1000000.0) + " ms for\n" + sql);
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

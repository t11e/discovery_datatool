package com.t11e.discovery.datatool;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public final class LoggingNamedParameterJdbcTemplate
  extends NamedParameterJdbcTemplate
{
  public static NamedParameterJdbcOperations create(final DataSource dataSource, final Logger logger, final Level level)
  {
    if (!logger.isLoggable(level))
    {
      return new NamedParameterJdbcTemplate(dataSource);
    }
    return new LoggingNamedParameterJdbcTemplate(dataSource, logger, level);
  }

  private final Logger logger;
  private final Level level;

  private LoggingNamedParameterJdbcTemplate(final DataSource dataSource, final Logger logger, final Level level)
  {
    super(dataSource);
    this.logger = logger;
    this.level = level;
  }

  @Override
  protected PreparedStatementCreator getPreparedStatementCreator(final String sql,
    final SqlParameterSource paramSource)
  {
    final ParsedSql parsedSql = getParsedSql(sql);
    final String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
    final Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
    if (logger.isLoggable(level)) // check again just in case the level was changed at runtime
    {
      logger.log(level, "Executing SQL with " + params.length + " parameters:\n" + sqlToUse);
      if (params.length > 0)
      {
        logger.log(level, "SQL Parameters:");
        int offset = 0;
        for (final Object param : params)
        {
          logger.log(level, "  [" + offset++ + "] " + param);
        }
      }
    }
    final int[] paramTypes = NamedParameterUtils.buildSqlTypeArray(parsedSql, paramSource);
    final PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(sqlToUse, paramTypes);
    return pscf.newPreparedStatementCreator(params);
  }
}

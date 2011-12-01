package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.t11e.discovery.datatool.JsonUtil;

public class JsonColumnProcessor
  implements IColumnProcessor
{
  public static final JsonColumnProcessor INSTANCE = new JsonColumnProcessor();

  @Override
  public Object processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    Object result = null;
    final String value = rs.getString(column);
    if (!rs.wasNull())
    {
      try
      {
        result = JsonUtil.decode(value);
      }
      catch (final Exception e)
      {
        // Swallow
      }
    }
    return result;
  }
}

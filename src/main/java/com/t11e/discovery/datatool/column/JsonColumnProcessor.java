package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONValue;

public class JsonColumnProcessor
  implements IColumnProcessor
{
  public static final IColumnProcessor INSTANCE = new JsonColumnProcessor();

  public Object processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    Object result = null;
    final String value = rs.getString(column);
    if (!rs.wasNull())
    {
      try
      {
        result = JSONValue.parse(value);
      }
      catch (final Exception e)
      {
        // Swallow
      }
    }
    return result;
  }
}

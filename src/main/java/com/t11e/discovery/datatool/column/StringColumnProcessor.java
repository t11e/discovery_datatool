package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

public class StringColumnProcessor
  implements IColumnProcessor
{
  public static final StringColumnProcessor INSTANCE =
      new StringColumnProcessor();

  @Override
  public String processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    String value = rs.getString(column);
    if (rs.wasNull())
    {
      value = null;
    }
    else
    {
      value = StringUtils.trimToNull(value);
    }
    return value;
  }
}

package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BooleanColumnProcessor
  implements IColumnProcessor
{
  public static final IColumnProcessor INSTANCE =
      new BooleanColumnProcessor();

  @Override
  public String processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    final boolean value = rs.getBoolean(column);
    return rs.wasNull() ? null :
      value ? "1" : "0";
  }
}

package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.Format;

import org.apache.commons.lang.time.FastDateFormat;

public class TimeColumnProcessor
  implements IColumnProcessor
{
  public static final IColumnProcessor INSTANCE =
      new TimeColumnProcessor();

  private final Format format = FastDateFormat.getInstance("HH:mm:ss");

  @Override
  public String processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    String output;
    final Time time = rs.getTime(column);
    if (rs.wasNull())
    {
      output = null;
    }
    else
    {
      output = format.format(time);
    }
    return output;
  }
}

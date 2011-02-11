package com.t11e.discovery.datatool.column;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;

import org.apache.commons.lang.time.FastDateFormat;

public class DateColumnProcessor
  implements IColumnProcessor
{
  public static final IColumnProcessor INSTANCE =
    new DateColumnProcessor();

  private final Format format = FastDateFormat.getInstance("yyyy-MM-dd");

  @Override
  public Object processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    String output;
    final Date date = rs.getDate(column);
    if (rs.wasNull())
    {
      output = null;
    }
    else
    {
      output = format.format(date);
    }
    return output;
  }
}

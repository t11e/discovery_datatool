package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Format;
import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;

public class TimestampColumnProcessor
  implements IColumnProcessor
{
  public static final IColumnProcessor INSTANCE =
      new TimestampColumnProcessor();

  private final Format format = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS");

  @Override
  public Object processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    String output;
    final Timestamp timestamp = rs.getTimestamp(column);
    if (timestamp == null)
    {
      output = null;
    }
    else
    {
      final Date date = new Date(timestamp.getTime());
      output = format.format(date);
    }
    return output;
  }
}

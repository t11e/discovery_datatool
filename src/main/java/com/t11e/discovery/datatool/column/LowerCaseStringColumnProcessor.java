package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

public class LowerCaseStringColumnProcessor
  implements IColumnProcessor
{
  public static final IColumnProcessor INSTANCE =
      new LowerCaseStringColumnProcessor();

  @Override
  public Object processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    return StringUtils.lowerCase(StringColumnProcessor.INSTANCE.processColumn(rs, column));
  }

}

package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

public class UpperCaseStringColumnProcessor
  implements IColumnProcessor
{
  public static final IColumnProcessor INSTANCE =
      new UpperCaseStringColumnProcessor();

  @Override
  public Object processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    return StringUtils.upperCase(StringColumnProcessor.INSTANCE.processColumn(rs, column));
  }

}

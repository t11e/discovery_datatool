package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface IColumnProcessor
{
  public Object processColumn(ResultSet rs, int column)
    throws SQLException;
}

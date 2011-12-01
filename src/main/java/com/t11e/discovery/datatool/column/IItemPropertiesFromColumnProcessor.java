package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public interface IItemPropertiesFromColumnProcessor
{
  /**
   * Sets properties on target item from current row in resultset for given column and property name
   */
  public void processColumn(final Map<String, Object> target, ResultSet rs, int column, String propertyName)
    throws SQLException;
}

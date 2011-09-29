package com.t11e.discovery.datatool;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

public class ItemIdBuilder
{
  private final String idColumn;

  public ItemIdBuilder(final String idColumn)
  {
    this.idColumn = idColumn;
  }

  public String getId(final ResultSet rs)
    throws SQLException
  {
    return StringUtils.trimToEmpty(rs.getString(idColumn));
  }

  public String getIdColumn()
  {
    return idColumn;
  }
}

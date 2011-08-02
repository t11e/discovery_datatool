package com.t11e.discovery.datatool.column;

public class MergeColumns
{
  private final String keyColumn;
  private final String valueColumn;

  public MergeColumns(final String keyColumn, final String valueColumn)
  {
    this.keyColumn = keyColumn;
    this.valueColumn = valueColumn;
  }

  public String getKeyColumn()
  {
    return keyColumn;
  }

  public String getValueColumn()
  {
    return valueColumn;
  }
}

package com.t11e.discovery.datatool;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class SqlQueryTemplate
{
  private String selectClause;
  private String fromClause;
  private String whereClause;
  private String deltaDateConstraint;
  private String snapshotDateConstraint;
  private List<String> jsonColumnNames;
  private boolean useLowerCaseColumnNames;

  public String getSelectClause()
  {
    return selectClause;
  }
  public void setSelectClause(final String selectClause)
  {
    this.selectClause = selectClause;
  }
  public String getFromClause()
  {
    return fromClause;
  }
  public void setFromClause(final String fromClause)
  {
    this.fromClause = fromClause;
  }
  public String getWhereClause()
  {
    return whereClause;
  }
  public void setWhereClause(final String whereClause)
  {
    this.whereClause = whereClause;
  }
  public String getDeltaDateConstraint()
  {
    return deltaDateConstraint;
  }
  public void setDeltaDateConstraint(final String deltaDateConstraint)
  {
    this.deltaDateConstraint = deltaDateConstraint;
  }
  public String getSnapshotDateConstraint()
  {
    return snapshotDateConstraint;
  }
  public void setSnapshotDateConstraint(final String snapshotDateConstraint)
  {
    this.snapshotDateConstraint = snapshotDateConstraint;
  }
  public List<String> getJsonColumnNames()
  {
    return jsonColumnNames;
  }
  public void setJsonColumnNames(final String jsonColumnNames)
  {
    this.jsonColumnNames = Arrays.asList(StringUtils.split(jsonColumnNames, ", "));
  }
  public void setJsonColumnNames(final List<String> jsonColumnNames)
  {
    this.jsonColumnNames = jsonColumnNames;
  }
  public boolean isUseLowerCaseColumnNames()
  {
    return useLowerCaseColumnNames;
  }
  public void setUseLowerCaseColumnNames(final boolean useLowerCaseColumnNames)
  {
    this.useLowerCaseColumnNames = useLowerCaseColumnNames;
  }
}

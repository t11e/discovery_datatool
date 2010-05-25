package com.t11e.discovery.datatool;

public class ChangesetPublisher
{
  private String name;
  private ChangesetExtractor changesetExtractor;

  public String getName()
  {
    return name;
  }
  public void setName(final String name)
  {
    this.name = name;
  }
  public ChangesetExtractor getChangesetExtractor()
  {
    return changesetExtractor;
  }
  public void setChangesetExtractor(final ChangesetExtractor changesetExtractor)
  {
    this.changesetExtractor = changesetExtractor;
  }
}

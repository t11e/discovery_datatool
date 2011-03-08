package com.t11e.discovery.datatool;

import org.springframework.beans.factory.annotation.Required;

public class ChangesetPublisher
{
  private String name;
  private ChangesetExtractor changesetExtractor;
  private ChangesetProfileService changesetProfileService;

  public String getName()
  {
    return name;
  }

  @Required
  public void setName(final String name)
  {
    this.name = name;
  }

  public ChangesetExtractor getChangesetExtractor()
  {
    return changesetExtractor;
  }

  @Required
  public void setChangesetExtractor(final ChangesetExtractor changesetExtractor)
  {
    this.changesetExtractor = changesetExtractor;
  }

  public ChangesetProfileService getChangesetProfileService()
  {
    return changesetProfileService;
  }

  public void setChangesetProfileService(
    final ChangesetProfileService changesetProfileService)
  {
    this.changesetProfileService = changesetProfileService;
  }
}

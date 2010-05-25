package com.t11e.discovery.datatool;

import java.util.Map;

public class ChangesetPublisherManager
{
  private Map<String, ChangesetPublisher> publishers;

  public ChangesetPublisher getChangesetPublisher(final String publisherName)
  {
    return publishers.get(publisherName);
  }

  public void setPublishers(final Map<String, ChangesetPublisher> publishers)
  {
    this.publishers = publishers;
  }
}

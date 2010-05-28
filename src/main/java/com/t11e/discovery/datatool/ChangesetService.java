package com.t11e.discovery.datatool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChangesetService
{
  @Autowired
  private ConfigurationManager configurationManager;

  public ChangesetPublisher getChangesetPublisher(
    final String name)
  {
    final ChangesetPublisherManager mgr = configurationManager.getBean(ChangesetPublisherManager.class);
    return mgr.getChangesetPublisher(name);
  }
}

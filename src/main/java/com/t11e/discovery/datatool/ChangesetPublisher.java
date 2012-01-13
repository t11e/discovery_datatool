package com.t11e.discovery.datatool;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;

public class ChangesetPublisher
  implements Validatable
{
  private String name;
  private ChangesetExtractor changesetExtractor;
  private ChangesetProfileService changesetProfileService;

  @Override
  public Collection<String> checkValid(final String context)
  {
    Collection<String> result = Collections.emptyList();
    if (changesetExtractor instanceof Validatable)
    {
      final Validatable cse = (Validatable) changesetExtractor;
      result = cse.checkValid((StringUtils.isNotBlank(context) ? context + " publisher " : "publisher ") + name);
    }
    return result;
  }

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

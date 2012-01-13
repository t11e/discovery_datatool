package com.t11e.discovery.datatool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ChangesetPublisherManager
  implements Validatable
{
  private Map<String, ChangesetPublisher> publishers;

  public ChangesetPublisher getChangesetPublisher(final String publisherName)
  {
    return publishers.get(publisherName);
  }

  public void setPublishers(final Collection<ChangesetPublisher> publishers)
  {
    this.publishers = new HashMap<String, ChangesetPublisher>(publishers.size());
    for (final ChangesetPublisher publisher : publishers)
    {
      final String name = publisher.getName();
      if (this.publishers.containsKey(name))
      {
        throw new IllegalArgumentException("More than one publisher has the name: " + name);
      }
      this.publishers.put(name, publisher);
    }
  }

  @Override
  public Collection<String> checkValid(final String context)
  {
    final Collection<String> result = new ArrayList<String>();
    for (final ChangesetPublisher publisher : publishers.values())
    {
      result.addAll(publisher.checkValid(context));
    }
    return result;
  }
}

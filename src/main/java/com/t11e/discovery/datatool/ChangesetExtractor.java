package com.t11e.discovery.datatool;

import java.util.Date;

public interface ChangesetExtractor
{
  void writeChangeset(
    ChangesetWriter writer,
    String changesetType,
    Date start,
    Date end);

  String determineType(Date start);
}

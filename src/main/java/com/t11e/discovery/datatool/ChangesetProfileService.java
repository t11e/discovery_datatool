package com.t11e.discovery.datatool;

import java.util.Date;

public interface ChangesetProfileService
{
  Date[] getChangesetProfileDateRange(String profile, boolean dryRun);

  void saveChangesetProfileLastRun(String profile, Date end);
}

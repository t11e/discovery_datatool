package com.t11e.discovery.datatool;

import org.apache.commons.lang.time.StopWatch;

public class StopWatchHelper
{

  public static StopWatch startTimer(final boolean shouldLogTiming)
  {
    StopWatch watch = null;
    if (shouldLogTiming)
    {
      watch = new StopWatch();
      watch.start();
    }
    return watch;
  }

}

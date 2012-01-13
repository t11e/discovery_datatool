package com.t11e.discovery.datatool;

public interface ProgressLogger
{
  /**
   * Begins reporting progress.
   */
  ProgressLogger begin(String description);

  /**
   * Ends reporting progress.
   */
  ProgressLogger done();

  /**
   * Reports an amount of work performed.
   */
  ProgressLogger worked(long worked);

  /**
   * Modifies estimated amount of work.
   */
  ProgressLogger setUnits(String unitSingle, String unitPlural);

  /**
   * Modifies estimated amount of work.
   */
  ProgressLogger setEstimatedWork(long estimatedWork, String unitSingle, String unitPlural);

  /**
   * Sets description of work currently begin done.
   */
  ProgressLogger setDescription(String description);

  /**
   * Sets whether the progress is being prevented by a failure. It is
   * possible to unset a failure after it has been set. Failure does not imply
   * completion; {@link #done()} must still be called to flag completion.
   */
  ProgressLogger setFailed(boolean failed);

  /**
   * Reports an exception.
   */
  ProgressLogger addException(Throwable throwable);
}

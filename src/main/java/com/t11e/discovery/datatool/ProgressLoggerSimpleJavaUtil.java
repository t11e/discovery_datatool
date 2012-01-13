package com.t11e.discovery.datatool;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

public class ProgressLoggerSimpleJavaUtil
  implements ProgressLogger
{
  private final NumberFormat format;
  private final Logger logger;
  private final Level level;
  /**
   * Don't log progress messages more often than this number of ms.
   */
  private final long minLogIntervalNs;
  private boolean inProgress;
  private long startNs;
  private long progress;
  private String description;
  private long lastLogMessgeTime;
  private long lastProgress;
  private boolean failed;
  private String unitSingle;
  private String unitPlural;
  private long estimatedWork;

  public ProgressLoggerSimpleJavaUtil(final Logger logger, final Level level, final long minLogIntervalMs)
  {
    format = NumberFormat.getNumberInstance();
    format.setMaximumFractionDigits(3);
    format.setGroupingUsed(true);
    this.logger = logger;
    this.level = level;
    minLogIntervalNs = TimeUnit.NANOSECONDS.convert(minLogIntervalMs, TimeUnit.MILLISECONDS);
  }

  @Override
  public ProgressLogger begin(final String description)
  {
    if (inProgress)
    {
      setFailed(true);
      done();
    }
    inProgress = true;
    progress = 0;
    lastProgress = 0;
    estimatedWork = 0;
    startNs = System.nanoTime();
    this.description = description;
    logProgress(startNs, "[begin]");
    return this;
  }

  @Override
  public ProgressLogger done()
  {
    inProgress = false;
    final long now = System.nanoTime();
    logProgress(now);
    failed = false;
    return this;
  }

  @Override
  public ProgressLogger worked(final long worked)
  {
    progress += worked;
    maybeLogProgress();
    return this;
  }

  @Override
  public ProgressLogger setUnits(final String unitSingle, final String unitPlural)
  {
    this.unitSingle = unitSingle;
    this.unitPlural = unitPlural;
    maybeLogProgress();
    return this;
  }

  @Override
  public ProgressLogger setEstimatedWork(final long estimatedWork, final String unitSingle, final String unitPlural)
  {
    this.estimatedWork = estimatedWork;
    this.unitSingle = unitSingle;
    this.unitPlural = unitPlural;
    maybeLogProgress();
    return this;
  }

  @Override
  public ProgressLogger setDescription(final String description)
  {
    this.description = description;
    maybeLogProgress();
    return this;
  }

  @Override
  public ProgressLogger setFailed(final boolean failed)
  {
    this.failed = failed;
    maybeLogProgress();
    return this;
  }

  @Override
  public ProgressLogger addException(final Throwable throwable)
  {
    final long now = System.nanoTime();
    final StringBuilder msg = new StringBuilder();
    formatProgressBar(msg, now);
    msg.append(" [exception]");
    logger.log(level, msg.toString(), throwable);
    lastLogMessgeTime = now;
    lastProgress = progress;
    return this;
  }

  private void maybeLogProgress()
  {
    if (inProgress)
    {
      final long now = System.nanoTime();
      if (now - lastLogMessgeTime > minLogIntervalNs)
      {
        logProgress(now);
      }
    }
  }

  private void logProgress(final long now, final String... extraLogContext)
  {
    final StringBuilder msg = new StringBuilder();
    formatProgressBar(msg, now);
    for (final String ctx : extraLogContext)
    {
      msg.append(" ").append(ctx);
    }
    logger.log(level, msg.toString());
    lastLogMessgeTime = now;
    lastProgress = progress;
  }

  private void formatProgressBar(
    final StringBuilder buffer,
    final long nowNs)
  {
    long eta = 0;
    if (estimatedWork > 0)
    {
      if (progress > 0)
      {
        final long elapsed = nowNs - startNs;
        final long unitsLeft = estimatedWork - progress;
        eta = (long) (unitsLeft * (elapsed / (double) progress));
      }
    }
    final long timeDelta = nowNs - lastLogMessgeTime;
    final long workDelta = progress - lastProgress;

    buffer.append(description);
    if (inProgress && estimatedWork == 0)
    {
      buffer.append("...");
    }
    final boolean estimation = estimatedWork > 0;
    if (estimation)
    {
      buffer.append(" [");
      final int barLength = 15;
      final int fillChars = (int) (((double) progress / estimatedWork) * barLength);
      for (int i = 0; i < barLength; i++)
      {
        buffer.append(i < fillChars ? '#' : '.');
      }
      buffer.append("]");
    }
    if (failed)
    {
      buffer.append(" [failed]");
    }
    if (!inProgress)
    {
      buffer.append(" [done]");
    }
    if (estimatedWork > 1)
    {
      final int pc = (int) ((progress / (double) estimatedWork) * 100);
      buffer.append(StringUtils.rightPad(String.valueOf(pc), 6));
      buffer.append("%");
    }
    if (estimation)
    {
      buffer.append(" [");
      final String s = format.format(estimatedWork);
      buffer.append(StringUtils.rightPad(format.format(progress), s.length()));
      buffer.append("/");
      buffer.append(s);
      buffer.append("]");
    }
    else if (progress > 0)
    {
      buffer.append(" [");
      buffer.append(format.format(progress));
      buffer.append("]");
    }
    final long workTime = nowNs - startNs;
    final long workTimeRounded = roundNs(workTime);
    if (workTimeRounded > 0)
    {
      buffer.append(" (");
      buffer.append(TimingUtil.formatTimingConcise(workTimeRounded, TimingUtil.UNITS_NS));
      if (inProgress)
      {
        final long etaRounded = roundNs(eta);
        if (etaRounded > 0)
        {
          buffer.append(", ETA ");
          buffer.append(TimingUtil.formatTimingConcise(etaRounded, TimingUtil.UNITS_NS));
        }
      }
      if (!inProgress && progress > 0)
      {
        buffer.append(", ");
        buffer.append(TimingUtil.formatItemTime(workTime, TimingUtil.UNITS_NS, progress, unitSingle, unitPlural));
      }
      final int unitsPerSec = inProgress
        ? (timeDelta > 0
          ? (int) (workDelta / (timeDelta / (1.0 * TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS)))) : 0)
        : (workTime > 0
          ? (int) (progress / (workTime / (1.0 * TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS)))) : 0);
      if (unitsPerSec > 0)
      {
        buffer.append(", ");
        buffer.append(format.format(unitsPerSec));
        buffer.append(" ");
        buffer.append(unitPlural);
        buffer.append("/sec");
      }
      buffer.append(')');
    }
  }

  private static long roundNs(final long workTime)
  {
    return workTime - (workTime % 1000000000);
  }
}

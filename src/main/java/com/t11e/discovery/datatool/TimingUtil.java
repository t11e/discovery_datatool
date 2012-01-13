package com.t11e.discovery.datatool;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class TimingUtil
{
  public static final int UNITS_NS = 0;
  public static final int UNITS_MS = 1;
  public static final int UNITS_SEC = 2;
  public static final int UNITS_MIN = 3;
  public static final int UNITS_DAY = 4;
  public static final int UNITS_WEEK = 5;
  private static final TimeUnit[] units = {TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS, TimeUnit.SECONDS,
      TimeUnit.MINUTES, TimeUnit.DAYS};
  private static final long s_divisions[] = new long[]{1000000, 1000, 60,
      60, 24, 7};
  private static final String s_conciseUnit[] = new String[] {"w", "d", "h",
      "m", "s", "ms", "ns"};
  private static final String s_conciseUnits[] = s_conciseUnit;
  private static final String s_longUnit[] = new String[] {"week", "day",
      "hour", "minute", "second", "ms", "ns"};
  private static final String s_longUnits[] = new String[] {"weeks", "days",
      "hours", "minutes", "seconds", "ms", "ns"};

  private static final NumberFormat s_formatter = NumberFormat
    .getNumberInstance(Locale.US);

  static
  {
    s_formatter.setMaximumFractionDigits(3);
  }

  private TimingUtil()
  {
    // Prevent instantiation
  }

  /**
   * Calculates delta, accounting for wrapping. The margin is
   * {@link Long#MAX_VALUE} divided by two.
   */
  public static long delta(final long previous, final long next)
  {
    return (Math.abs(next - previous) > Long.MAX_VALUE / 2)
      ? (next < previous ? Long.MAX_VALUE - previous + next : Long.MAX_VALUE
        - next + previous) : next - previous;
  }

  public static String formatTiming(
    final double time,
    final int timeUnits,
    final int formatUnits)
  {
    return formatTiming((int) convertTime(time, timeUnits, formatUnits),
      formatUnits);
  }

  public static String formatTimingConcise(
    final double time,
    final int timeUnits,
    final int formatUnits)
  {
    return formatTimingConcise(
      (int) convertTime(time, timeUnits, formatUnits), formatUnits);
  }

  public static String formatTiming(final long time, final int units)
  {
    return formatTiming(time, units, " ", " ", s_longUnit, s_longUnits);
  }

  public static String formatTimingConcise(final long time, final int units)
  {
    return formatTiming(time, units, null, ":", s_conciseUnit, s_conciseUnits);
  }

  public static double convertTime(
    double time,
    final int sourceUnits,
    final int targetUnits)
  {
    if (sourceUnits < targetUnits)
    {
      for (int i = sourceUnits; i < targetUnits; i++)
      {
        time /= s_divisions[i];
      }
    }
    else if (targetUnits < sourceUnits)
    {
      for (int i = sourceUnits - 1; i >= targetUnits; i--)
      {
        time *= s_divisions[i];
      }
    }
    return time;
  }

  private static String formatTiming(
    long time,
    final int timeUnits,
    final String numUnitSep,
    final String partSep,
    final String[] unit,
    final String[] units)
  {
    final int parts[] = new int[s_divisions.length + 1 - timeUnits];
    {
      int index = parts.length - 1;
      for (int i = timeUnits; i < s_divisions.length; i++)
      {
        parts[index--] = (int) (time % s_divisions[i]);
        time /= s_divisions[i];
      }
    }
    parts[0] = (int) time;

    final StringBuilder buffer = new StringBuilder(32);
    final int last = parts.length - 1;
    int head = 0;
    while (head < last && parts[head] == 0)
    {
      head++;
    }
    int tail = last;
    while (tail > head && parts[tail] == 0)
    {
      tail--;
    }
    if (head == tail && head < last && parts[head] == 0)
    {
      head = tail = last;
    }
    for (int i = head; i <= tail; i++)
    {
      if (partSep != null && buffer.length() > 0)
      {
        buffer.append(partSep);
      }
      buffer.append(parts[i]);
      if (numUnitSep != null)
      {
        buffer.append(numUnitSep);
      }
      if (parts[i] == 1)
      {
        buffer.append(unit[i]);
      }
      else
      {
        buffer.append(units[i]);
      }
    }
    return buffer.toString();
  }

  public static String formatItemTime(
    final long time,
    final int timeUnit,
    final long count,
    final String unitSingular,
    final String unitPlural)
  {
    return formatItemTime(time, timeUnit, count, unitSingular, unitPlural, false);
  }

  public static String formatItemTimeIfMultiple(
    final long time,
    final int timeUnit,
    final long count,
    final String unitSingular,
    final String unitPlural)
  {
    return formatItemTime(time, timeUnit, count, unitSingular, unitPlural, true);
  }

  private static String formatItemTime(
    final long time,
    final int timeUnit,
    final long count,
    final String unitSingular,
    final String unitPlural,
    final boolean onlyIfMultiple)
  {
    double itemsPerSec = count / (time / (1.0 * unitsPerSecond(timeUnit)));
    if (Double.isNaN(itemsPerSec) || Double.isInfinite(itemsPerSec))
    {
      itemsPerSec = 0.0;
    }
    final long msPerItem = (long) ((double) time / (double) count);
    final StringBuilder buffer = new StringBuilder();
    boolean comma = false;
    if (!onlyIfMultiple || count > 1)
    {
      buffer.append(s_formatter.format(count));
      buffer.append(" ");
      buffer.append(count == 1 ? unitSingular : unitPlural);
      comma = true;
    }
    if (itemsPerSec > 0.0)
    {
      if (comma)
      {
        buffer.append(", ");
      }
      buffer.append(s_formatter.format(itemsPerSec));
      buffer.append(" ");
      buffer.append(itemsPerSec == 1 ? unitSingular : unitPlural);
      buffer.append("/sec");
    }
    if (count > 1 && msPerItem > 0)
    {
      if (comma)
      {
        buffer.append(", ");
      }
      buffer.append(TimingUtil.formatTimingConcise(msPerItem, timeUnit));
      buffer.append("/");
      buffer.append(unitSingular);
    }
    return buffer.toString();
  }

  private static long unitsPerSecond(final int timeUnit)
  {
    return units[timeUnit].convert(1, TimeUnit.SECONDS);
  }

  public static String formatPercentage(double percent)
  {
    if (Double.isNaN(percent))
    {
      percent = 0.0;
    }
    return NumberFormat.getPercentInstance().format(percent);
  }

  public static String formatPercentage(final long numerator, final long denominator)
  {
    final double percent = (double) numerator / (double) denominator;
    return formatPercentage(percent);
  }
}

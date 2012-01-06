package com.t11e.discovery.datatool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.apache.commons.lang.StringUtils;

public class CustomLogFormatter
  extends Formatter
{
  private static final DateFormat s_dateFormat = new SimpleDateFormat(
    "yyyyMMdd HH:mm:ss,SSS");

  @Override
  public String format(final LogRecord record)
  {
    final StringBuilder buffer = new StringBuilder();
    synchronized (s_dateFormat)
    {
      final Date date = new Date(record.getMillis());
      buffer.append('[');
      buffer.append(s_dateFormat.format(date));
      buffer.append("] ");
    }
    buffer.append("[");
    {
      final String threadId = Integer.toHexString(record.getThreadID());
      buffer.append(StringUtils.leftPad(threadId, 8, '0'));
    }
    buffer.append("] ");
    buffer.append("[");
    buffer.append(StringUtils.rightPad(record.getLevel().getName(), 7));
    buffer.append("] ");
    final String loggerName = record.getLoggerName();
    buffer.append("[");
    buffer.append(StringUtils.rightPad(loggerName, 40));
    buffer.append("] ");
    {
      final String message = record.getMessage();
      final int prefixLength = buffer.length();
      int start = 0;
      final int length = message != null ? message.length() : 0;
      if (message != null)
      {
        while (start < length)
        {
          int eol = message.indexOf('\n', start);
          if (eol == -1)
          {
            eol = message.length();
          }
          if (start > 0)
          {
            buffer.append(buffer.substring(0, prefixLength));
          }
          buffer.append(message.substring(start, eol));
          buffer.append('\n');
          start = eol + 1;
        }
      }
    }
    final Throwable t = record.getThrown();
    if (t != null)
    {
      buffer.append(t.getClass().getName());
      buffer.append(": ");
      buffer.append(getStackTrace(t));
      buffer.append('\n');
    }
    return buffer.toString();
  }

  private static String getStackTrace(final Throwable throwable)
  {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    pw.close();
    return sw.toString();
  }
}

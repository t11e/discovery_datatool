package com.t11e.discovery.datatool;

import org.apache.commons.lang.StringUtils;

public enum PropertyCase
{
  PRESERVE
  {
    @Override
    public String convert(final String in)
    {
      return in;
    }
  },
  LOWER
  {
    @Override
    public String convert(final String in)
    {
      return StringUtils.lowerCase(in);
    }
  },
  UPPER
  {
    @Override
    public String convert(final String in)
    {
      return StringUtils.upperCase(in);
    }
  },
  LEGACY
  {
    @Override
    public String convert(final String in)
    {
      return StringUtils.lowerCase(in);
    }
  }
  ;

  public abstract String convert(final String in);
}

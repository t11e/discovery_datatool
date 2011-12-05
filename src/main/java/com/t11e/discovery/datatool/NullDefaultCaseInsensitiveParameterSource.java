package com.t11e.discovery.datatool;

import java.util.Map;


public class NullDefaultCaseInsensitiveParameterSource
  extends CaseInsensitveParameterSource
{

  public NullDefaultCaseInsensitiveParameterSource()
  {
    super();
  }

  public NullDefaultCaseInsensitiveParameterSource(final Map<String, Object> params)
  {
    super(params);
  }

  public NullDefaultCaseInsensitiveParameterSource(final String paramName, final Object value)
  {
    super(paramName, value);
  }

  @Override
  public boolean hasValue(final String paramName)
  {
    return true;
  }
}

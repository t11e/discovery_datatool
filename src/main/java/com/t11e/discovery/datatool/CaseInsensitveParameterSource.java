package com.t11e.discovery.datatool;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.Assert;

/**
 * Based on {@link MapSqlParameterSource}, but backed by a {@link CaseInsensitiveMap}.
 */
public class CaseInsensitveParameterSource
  extends AbstractSqlParameterSource
{
  @SuppressWarnings("unchecked")
  private final Map<String, Object> values = new CaseInsensitiveMap();

  public CaseInsensitveParameterSource()
  {
  }

  public CaseInsensitveParameterSource(final String paramName, final Object value)
  {
    addValue(paramName, value);
  }

  public CaseInsensitveParameterSource(final Map<String, Object> params)
  {
    addValues(params);
  }

  /**
   * Add a parameter to this parameter source.
   * @param paramName the name of the parameter
   * @param value the value of the parameter
   * @return a reference to this parameter source,
   * so it's possible to chain several calls together
   */
  public CaseInsensitveParameterSource addValue(final String paramName, final Object value)
  {
    Assert.notNull(paramName, "Parameter name must not be null");
    values.put(paramName, value);
    if (value instanceof SqlParameterValue)
    {
      registerSqlType(paramName, ((SqlParameterValue) value).getSqlType());
    }
    return this;
  }

  /**
   * Add a parameter to this parameter source.
   * @param paramName the name of the parameter
   * @param value the value of the parameter
   * @param sqlType the SQL type of the parameter
   * @return a reference to this parameter source,
   * so it's possible to chain several calls together
   */
  public CaseInsensitveParameterSource addValue(final String paramName, final Object value, final int sqlType)
  {
    Assert.notNull(paramName, "Parameter name must not be null");
    values.put(paramName, value);
    registerSqlType(paramName, sqlType);
    return this;
  }

  /**
   * Add a parameter to this parameter source.
   * @param paramName the name of the parameter
   * @param value the value of the parameter
   * @param sqlType the SQL type of the parameter
   * @param typeName the type name of the parameter
   * @return a reference to this parameter source,
   * so it's possible to chain several calls together
   */
  public CaseInsensitveParameterSource addValue(final String paramName, final Object value, final int sqlType,
    final String typeName)
  {
    Assert.notNull(paramName, "Parameter name must not be null");
    values.put(paramName, value);
    registerSqlType(paramName, sqlType);
    registerTypeName(paramName, typeName);
    return this;
  }

  /**
   * Add a Map of parameters to this parameter source.
   * @param values a Map holding existing parameter values (can be <code>null</code>)
   * @return a reference to this parameter source,
   * so it's possible to chain several calls together
   */
  public CaseInsensitveParameterSource addValues(final Map<String, ? > values)
  {
    if (values != null)
    {
      for (final Map.Entry<String, ? > entry : values.entrySet())
      {
        this.values.put(entry.getKey(), entry.getValue());
        if (entry.getValue() instanceof SqlParameterValue)
        {
          final SqlParameterValue value = (SqlParameterValue) entry.getValue();
          registerSqlType(entry.getKey(), value.getSqlType());
        }
      }
    }
    return this;
  }

  /**
   * Expose the current parameter values as read-only Map.
   */
  public Map<String, Object> getValues()
  {
    return Collections.unmodifiableMap(values);
  }

  @Override
  public boolean hasValue(final String paramName)
  {
    return values.containsKey(paramName);
  }

  @Override
  public Object getValue(final String paramName)
  {
    if (!hasValue(paramName))
    {
      throw new IllegalArgumentException("No value registered for key '" + paramName + "'");
    }
    return values.get(paramName);
  }
}

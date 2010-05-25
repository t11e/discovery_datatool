package com.t11e.discovery.datatool;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CollectionsFactory
{
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> makeMapGeneric(final Object... keyValuePairs)
  {
    return makeMapFromArray(keyValuePairs);
  }

  @SuppressWarnings("unchecked")
  public static Map makeMap(final Object... keyValuePairs)
  {
    return makeMapFromArray(keyValuePairs);
  }

  @SuppressWarnings("unchecked")
  public static Map makeMapFromArray(final Object[] keyValuePairs)
  {
    final LinkedHashMap result = new LinkedHashMap();

    for (int i = 0; i < keyValuePairs.length; i += 2)
    {
      result.put(keyValuePairs[i], keyValuePairs[i+1]);
    }
    return result;
  }

  public static <T> List<T> makeList(final T...values)
  {
    return Arrays.asList(values);
  }
}

package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.t11e.discovery.datatool.PropertyCase;

/**
 * If json column contains a map, then merges that map with the target item properties for that row, converting key case based on
 * configured PropertyCase. If json column is not a map, then value is stored in a single key (propertyName) in the item map.
 */
public class ItemPropertiesFromUnscopedJsonColumnProcessor
  implements IItemPropertiesFromColumnProcessor
{
  private final JsonColumnProcessor delegate;
  private final PropertyCase propertyCase;

  public ItemPropertiesFromUnscopedJsonColumnProcessor(final JsonColumnProcessor delegate, final PropertyCase propertyCase)
  {
    this.delegate = delegate;
    this.propertyCase = propertyCase;
  }

  @Override
  public void processColumn(final Map<String, Object> target, final ResultSet rs, final int column,
    final String propertyName)
    throws SQLException
  {
    final Object value = delegate.processColumn(rs, column);
    if (value != null)
    {
      if (value instanceof Map)
      {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map) value;
        if (propertyCase != PropertyCase.PRESERVE)
        {
          for (final String key : new ArrayList<String>(map.keySet()))
          {
            final String newKey = propertyCase.convert(key);
            if (!StringUtils.equals(key, newKey))
            {
              map.put(newKey, map.remove(key));
            }
          }
        }
        target.putAll(map);
      }
      else
      {
        target.put(propertyName, value);
      }
    }
  }

}

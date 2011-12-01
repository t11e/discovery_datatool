package com.t11e.discovery.datatool;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import com.t11e.discovery.datatool.column.MergeColumns;

public class SqlAction
  implements InitializingBean
{
  private Set<String> filter = Collections.singleton("any");
  private String action;
  private String query;
  private List<SubQuery> subqueries;
  private List<MergeColumns> mergeColumns;
  private String idColumn;
  private String providerColumn;
  private String kindColumn;
  private Set<String> scopedJsonColumns = Collections.emptySet();
  private Set<String> unscopedJsonColumns = Collections.emptySet();
  private PropertyCase propertyCase;

  @Override
  public void afterPropertiesSet()
    throws Exception
  {
    if (mergeColumns != null)
    {
      for (final ListIterator<MergeColumns> it = mergeColumns.listIterator(); it.hasNext();)
      {
        final MergeColumns merge = it.next();
        it.set(new MergeColumns(propertyCase.convert(merge.getKeyColumn()), propertyCase.convert(merge.getValueColumn())));
      }
    }
    final Transformer transformer = new Transformer()
    {
      @Override
      public String transform(final Object in)
      {
        return StringUtils.lowerCase((String) in);
      }
    };
    CollectionUtils.transform(scopedJsonColumns, transformer);
    CollectionUtils.transform(unscopedJsonColumns, transformer);
  }

  public Set<String> getFilter()
  {
    return filter;
  }

  public void setFilter(final Set<String> filter)
  {
    this.filter = filter;
  }

  public void setFilter(final String filters)
  {
    final String[] tokens = StringUtils.split(filters, ", ");
    if (tokens == null)
    {
      filter = Collections.emptySet();
    }
    else
    {
      filter = new HashSet<String>(Arrays.asList(tokens));
    }
  }

  public String getAction()
  {
    return action;
  }

  @Required
  public void setAction(final String action)
  {
    this.action = action;
  }

  public String getQuery()
  {
    return query;
  }

  @Required
  public void setQuery(final String query)
  {
    this.query = query;
  }

  @Required
  public void setIdColumn(final String idColumn)
  {
    this.idColumn = idColumn;
  }

  public String getIdColumn()
  {
    return idColumn;
  }

  public Set<String> getScopedJsonColumnsSet()
  {
    return scopedJsonColumns;
  }

  public void setScopedJsonColumns(final String scopedJsonColumns)
  {
    this.scopedJsonColumns = new LinkedHashSet<String>(Arrays.asList(StringUtils.split(scopedJsonColumns, ", ")));
  }

  public Set<String> getUnscopedJsonColumnsSet()
  {
    return unscopedJsonColumns;
  }

  public void setUnscopedJsonColumns(final String unscopedJsonColumns)
  {
    this.unscopedJsonColumns = new LinkedHashSet<String>(Arrays.asList(StringUtils.split(unscopedJsonColumns, ", ")));
  }

  public PropertyCase getPropertyCase()
  {
    return propertyCase;
  }

  public void setPropertyCase(final PropertyCase propertyCase)
  {
    this.propertyCase = propertyCase;
  }

  public String getProviderColumn()
  {
    return providerColumn;
  }

  public void setProviderColumn(final String providerColumn)
  {
    this.providerColumn = providerColumn;
  }

  public String getKindColumn()
  {
    return kindColumn;
  }

  public void setKindColumn(final String kindColumn)
  {
    this.kindColumn = kindColumn;
  }

  public void setMergeColumns(final List<MergeColumns> mergeColumns)
  {
    this.mergeColumns = mergeColumns;
  }

  public List<MergeColumns> getMergeColumns()
  {
    return mergeColumns;
  }

  public void setSubqueries(final List<SubQuery> subqueries)
  {
    this.subqueries = subqueries;
  }

  public List<SubQuery> getSubqueries()
  {
    return subqueries;
  }
}

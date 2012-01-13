package com.t11e.discovery.datatool;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;

public class DeleteActionRowCallbackHandler
  implements CompletionAwareRowCallbackHandler
{
  private final ChangesetWriter writer;
  private final ItemIdBuilder itemIdBuilder;
  private final String providerColumn;
  private final String kindColumn;
  private final ProgressLogger progress;

  public DeleteActionRowCallbackHandler(
    final ChangesetWriter writer,
    final String idColumn,
    final String providerColumn,
    final String kindColumn,
    final ProgressLogger progress)
  {
    this.writer = writer;
    this.providerColumn = providerColumn;
    this.kindColumn = kindColumn;
    this.progress = progress;
    itemIdBuilder = new ItemIdBuilder(idColumn);
  }

  @Override
  public void processRow(final ResultSet rs)
    throws SQLException
  {
    final String id = itemIdBuilder.getId(rs);
    try
    {
      if (providerColumn != null || kindColumn != null)
      {
        final String provider = StringUtils.trimToEmpty(rs.getString(providerColumn));
        final String kind = StringUtils.trimToEmpty(rs.getString(kindColumn));
        writer.removeItem(id, provider, kind);
      }
      else
      {
        writer.removeItem(id);
      }
      progress.worked(1);
    }
    catch (final XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void flushItem()
  {
    // do nothing
  }
}

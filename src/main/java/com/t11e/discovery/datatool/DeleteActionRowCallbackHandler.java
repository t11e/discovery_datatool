package com.t11e.discovery.datatool;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.stream.XMLStreamException;

public class DeleteActionRowCallbackHandler
  implements CompletionAwareRowCallbackHandler
{
  private final ChangesetWriter writer;
  private final ItemIdBuilder itemIdBuilder;

  public DeleteActionRowCallbackHandler(
    final ChangesetWriter writer,
    final String idColumn)
  {
    this.writer = writer;
    itemIdBuilder = new ItemIdBuilder(idColumn);
  }

  @Override
  public void processRow(final ResultSet rs)
    throws SQLException
  {
    final String id = itemIdBuilder.getId(rs);
    try
    {
      writer.removeItem(id);
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

package com.t11e.discovery.datatool;

import java.util.Map;

import javax.xml.stream.XMLStreamException;

public interface ChangesetWriter
{
  public void setItem(final String id, final Map<String, ? > properties)
    throws XMLStreamException;

  public void addToItem(final String id, final Map<String, ? > properties)
    throws XMLStreamException;

  public void removeFromItem(final String id, final Map<String, ? > properties)
    throws XMLStreamException;

  public void removeAllFromItem(final String id)
    throws XMLStreamException;

  public void addItem(final String id)
    throws XMLStreamException;

  public void removeItem(final String id)
    throws XMLStreamException;
}

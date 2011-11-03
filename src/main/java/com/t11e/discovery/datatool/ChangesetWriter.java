package com.t11e.discovery.datatool;

import java.util.Map;

import javax.xml.stream.XMLStreamException;

public interface ChangesetWriter
{
  void setItem(final String id, final Map<String, ? > properties)
    throws XMLStreamException;

  void setItem(final String id, final String provider, final String kind, final Map<String, ? > properties)
    throws XMLStreamException;

  void addToItem(final String id, final Map<String, ? > properties)
    throws XMLStreamException;

  void addToItem(final String id, final String provider, final String kind, final Map<String, ? > properties)
    throws XMLStreamException;

  void removeFromItem(final String id, final Map<String, ? > properties)
    throws XMLStreamException;

  void removeAllFromItem(final String id)
    throws XMLStreamException;

  void addItem(final String id)
    throws XMLStreamException;

  void removeItem(final String id)
    throws XMLStreamException;

  void removeItem(final String id, final String provider, final String kind)
    throws XMLStreamException;
}

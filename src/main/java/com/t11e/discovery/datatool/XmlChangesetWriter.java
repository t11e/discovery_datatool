package com.t11e.discovery.datatool;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class XmlChangesetWriter
  implements ChangesetWriter
{
  private final XMLStreamWriter writer;

  public XmlChangesetWriter(final XMLStreamWriter writer)
  {
    this.writer = writer;
  }

  public void setItem(
    final String id,
    final Map<String, Object> properties)
    throws XMLStreamException
  {
    writer.writeStartElement("set-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(id, properties, false);
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public void addToItem(
    final String id,
    final Map<String, Object> properties)
    throws XMLStreamException
  {
    writer.writeStartElement("add-to-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(id, properties, false);
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public void removeFromItem(
    final String id,
    final Map<String, Object> properties)
    throws XMLStreamException
  {
    writer.writeStartElement("remove-from-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(id, properties, true);
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public void removeAllFromItem(
    final String id)
    throws XMLStreamException
  {
    writer.writeStartElement("remove-from-item");
    writer.writeAttribute("id", id);
    writer.writeEmptyElement("all");
    writer.writeEndElement();
  }

  public void addItem(
    final String id)
    throws XMLStreamException
  {
    writer.writeStartElement("add-item");
    writer.writeAttribute("id", id);
    writer.writeEndElement();
  }

  public void removeItem(
    final String id)
    throws XMLStreamException
  {
    writer.writeStartElement("remove-item");
    writer.writeAttribute("id", id);
    writer.writeEndElement();
  }

  @SuppressWarnings("unchecked") void writeValue(
    final String id,
    final Object object,
    final boolean allowEmpty)
    throws XMLStreamException
  {
    if (object instanceof Map)
    {
      final Map struct = (Map) object;
      writer.writeStartElement("struct");
      for (final Iterator i = struct.entrySet().iterator(); i.hasNext(); )
      {
        final Map.Entry entry = (Map.Entry) i.next();
        final String name = entry.getKey().toString();
        final Object value = entry.getValue();
        writer.writeStartElement("entry");
        writer.writeAttribute("name", name);
        if (value != null || !allowEmpty)
        {
          writeValue(id + ":" + name, value, allowEmpty);
        }
        writer.writeEndElement();
      }
      writer.writeEndElement();
    }
    else if (object instanceof Collection)
    {
      final Collection array = (Collection) object;
      writer.writeStartElement("array");
      for (final Iterator i = array.iterator(); i.hasNext(); )
      {
        final Object value = i.next();
        writer.writeStartElement("element");
        writeValue(id, value, allowEmpty);
        writer.writeEndElement();
      }
      writer.writeEndElement();
    }
    else if (object instanceof String)
    {
      final String value = (String) object;
      writer.writeStartElement("string");
      writer.writeCharacters(value);
      writer.writeEndElement();
    }
    else if (object instanceof Integer)
    {
      final Integer value = (Integer) object;
      writer.writeStartElement("int");
      writer.writeCharacters(value.toString());
      writer.writeEndElement();
    }
    else if (object instanceof Double)
    {
      final Double value = (Double) object;
      writer.writeStartElement("real");
      writer.writeCharacters(value.toString());
      writer.writeEndElement();
    }
    else if (object instanceof Boolean)
    {
      final Boolean value = (Boolean) object;
      writer.writeStartElement("bool");
      writer.writeCharacters(value.booleanValue() ? "1" : "0");
      writer.writeEndElement();
    }
    else
    {
      throw new XMLStreamException("Unable to write object with id " + id + ": " +
        (object == null
          ? "NULL"
          : (object.getClass().getName() + ": " + object)));
    }
  }

}

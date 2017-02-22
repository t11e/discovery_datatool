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
  private int numSetItem;
  private int numSetItemProviderKind;
  private int numAddToItem;
  private int numAddToItemProviderKind;
  private int numRemoveFromItem;
  private int numRemoveAllFromItem;
  private int numAddItem;
  private int numRemoveItem;
  private int numRemoveItemProviderKind;

  public XmlChangesetWriter(final XMLStreamWriter writer)
  {
    this.writer = writer;
  }

  public String summarizeActions()
  {
    final StringBuilder builder = new StringBuilder();
    summarizeAction(builder, "set-item", numSetItem);
    summarizeAction(builder, "set-item-provider-kind", numSetItemProviderKind);
    summarizeAction(builder, "add-to-item", numAddToItem);
    summarizeAction(builder, "add-to-item-provider-kind", numAddToItemProviderKind);
    summarizeAction(builder, "remove-from-item", numRemoveFromItem);
    summarizeAction(builder, "remove-from-item-all", numRemoveAllFromItem);
    summarizeAction(builder, "add-item", numAddItem);
    summarizeAction(builder, "remove-item", numRemoveItem);
    summarizeAction(builder, "remove-item-provider-kind", numRemoveItemProviderKind);
    if (builder.length() == 0) {
      return "empty changeset";
    }
    return builder.toString();
  }

  private static void summarizeAction(final StringBuilder builder, final String desc, final int count)
  {
    if (count != 0)
    {
      if (builder.length() > 0)
      {
        builder.append(" ");
      }
      builder.append(desc).append("=").append(count);
    }
  }

  @Override
  public void setItem(
    final String id,
    final Map<String, ? > properties)
    throws XMLStreamException
  {
    numSetItem++;
    writer.writeStartElement("set-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(id, properties, true);
    writer.writeEndElement();
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  @Override
  public void setItem(final String id, final String provider, final String kind, final Map<String, ? > properties)
    throws XMLStreamException
  {
    numSetItemProviderKind++;
    writer.writeStartElement("set-item");
    writer.writeAttribute("locator", id);
    writer.writeAttribute("provider", provider);
    writer.writeAttribute("kind", kind);
    writer.writeStartElement("properties");
    writeValue(id, properties, true);
    writer.writeEndElement();
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  @Override
  public void addToItem(
    final String id,
    final Map<String, ? > properties)
    throws XMLStreamException
  {
    numAddToItem++;
    writer.writeStartElement("add-to-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(id, properties, false);
    writer.writeEndElement();
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  @Override
  public void addToItem(final String id, final String provider, final String kind, final Map<String, ? > properties)
    throws XMLStreamException
  {
    numAddToItemProviderKind++;
    writer.writeStartElement("add-to-item");
    writer.writeAttribute("locator", id);
    writer.writeAttribute("provider", provider);
    writer.writeAttribute("kind", kind);
    writer.writeStartElement("properties");
    writeValue(id, properties, false);
    writer.writeEndElement();
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  @Override
  public void removeFromItem(
    final String id,
    final Map<String, ? > properties)
    throws XMLStreamException
  {
    numRemoveFromItem++;
    writer.writeStartElement("remove-from-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(id, properties, true);
    writer.writeEndElement();
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  @Override
  public void removeAllFromItem(
    final String id)
    throws XMLStreamException
  {
    numRemoveAllFromItem++;
    writer.writeStartElement("remove-from-item");
    writer.writeAttribute("id", id);
    writer.writeEmptyElement("all");
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  @Override
  public void addItem(
    final String id)
    throws XMLStreamException
  {
    numAddItem++;
    writer.writeStartElement("add-item");
    writer.writeAttribute("id", id);
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  @Override
  public void removeItem(
    final String id)
    throws XMLStreamException
  {
    numRemoveItem++;
    writer.writeStartElement("remove-item");
    writer.writeAttribute("id", id);
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  @Override
  public void removeItem(final String id, final String provider, final String kind)
    throws XMLStreamException
  {
    numRemoveItemProviderKind++;
    writer.writeStartElement("remove-item");
    writer.writeAttribute("locator", id);
    writer.writeAttribute("provider", provider);
    writer.writeAttribute("kind", kind);
    writer.writeEndElement();
    writer.writeCharacters("\n");
  }

  void writeValue(
    final String id,
    final Object object,
    final boolean allowEmpty)
    throws XMLStreamException
  {
    if (object instanceof Map)
    {
      final Map struct = (Map) object;
      writer.writeStartElement("struct");
      for (final Iterator i = struct.entrySet().iterator(); i.hasNext();)
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
      for (final Iterator i = array.iterator(); i.hasNext();)
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
      StaxUtil.writeFilteredCharacters(writer, value);
      writer.writeEndElement();
    }
    else if (object instanceof Integer)
    {
      final Integer value = (Integer) object;
      writer.writeStartElement("int");
      writer.writeCharacters(value.toString());
      writer.writeEndElement();
    }
    else if (object instanceof Long)
    {
      final Long value = (Long) object;
      writer.writeStartElement("long");
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

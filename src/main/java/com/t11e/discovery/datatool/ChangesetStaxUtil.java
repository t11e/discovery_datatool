package com.t11e.discovery.datatool;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * Utility class to use StAX to read/write changesets.
 */
public class ChangesetStaxUtil
  extends StaxUtil
{
  private ChangesetStaxUtil()
  {
    // Prevent instantiation
  }

  public static void parseChangeset(final InputStream is, final IItemChangesetListener listener)
    throws XMLStreamException
  {
    final XMLInputFactory factory = StaxUtil.newInputFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    final XMLStreamReader reader = factory.createXMLStreamReader(is);
    try
    {
      // Start parsing on the first tag
      StaxUtil.nextTagIgnoringDocType(reader);
      parseChangeset(reader, listener);
    }
    finally
    {
      try
      {
        reader.close();
      }
      catch (final Exception e)
      {
        // Swallow
      }
    }
  }

  public static void parseChangeset(final XMLStreamReader reader,
    final IItemChangesetListener listener)
    throws XMLStreamException
  {
    reader.require(XMLStreamConstants.START_ELEMENT, null, "changeset");
    listener.onStartChangeset();
    while (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
    {
      parseChangesetAction(reader, listener);
    }
    reader.require(XMLStreamConstants.END_ELEMENT, null, "changeset");
    listener.onEndChangeset();
  }

  public static void parseChangesetAction(final XMLStreamReader reader,
    final IItemChangesetListener listener)
  throws XMLStreamException
  {
    reader.require(XMLStreamConstants.START_ELEMENT, null, null);
    final String name = reader.getLocalName();
    if (name.equals("set-item"))
    {
      final String id = StaxUtil.getRequiredAttributeValue(reader, null, "id");
      if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
      {
        reader.require(XMLStreamConstants.START_ELEMENT, null, "properties");
        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
        {
          final Map properties = readProperties(reader, false);
          reader.nextTag();
          listener.onSetItem(id, properties);
        }
        reader.require(XMLStreamConstants.END_ELEMENT, null, "properties");
        reader.nextTag();
      }
      reader.require(XMLStreamConstants.END_ELEMENT, null, "set-item");
    }
    else if (name.equals("add-to-item"))
    {
      final String id = StaxUtil.getRequiredAttributeValue(reader, null, "id");
      if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
      {
        reader.require(XMLStreamConstants.START_ELEMENT, null, "properties");
        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
        {
          final Map properties = readProperties(reader, false);
          listener.onAddToItem(id, properties);
          reader.nextTag();
        }
        reader.require(XMLStreamConstants.END_ELEMENT, null, "properties");
        reader.nextTag();
      }
      reader.require(XMLStreamConstants.END_ELEMENT, null, "add-to-item");
    }
    else if (name.equals("remove-from-item"))
    {
      final String id = StaxUtil.getRequiredAttributeValue(reader, null, "id");
      if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
      {
        final String localName = reader.getLocalName();
        if (localName.equals("all"))
        {
          reader.nextTag();
          listener.onRemoveFromItem(id, IItemChangesetListener.ALL);
          reader.require(XMLStreamConstants.END_ELEMENT, null, "all");
        }
        else
        {
          reader.require(XMLStreamConstants.START_ELEMENT, null, "properties");
          if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
          {
            final Map properties = readProperties(reader, true);
            listener.onRemoveFromItem(id, properties);
            reader.nextTag();
          }
          reader.require(XMLStreamConstants.END_ELEMENT, null, "properties");
        }
        reader.nextTag();
      }
      reader.require(XMLStreamConstants.END_ELEMENT, null, "remove-from-item");
    }
    else if (name.equals("add-item"))
    {
      final String id = StaxUtil.getRequiredAttributeValue(reader, null, "id");
      listener.onAddItem(id);
      reader.nextTag();
      reader.require(XMLStreamConstants.END_ELEMENT, null, "add-item");
    }
    else if (name.equals("remove-item"))
    {
      final String id = StaxUtil.getRequiredAttributeValue(reader, null, "id");
      listener.onRemoveItem(id);
      reader.nextTag();
      reader.require(XMLStreamConstants.END_ELEMENT, null, "remove-item");
    }
  }

  private static Map readProperties(final XMLStreamReader reader, final boolean allowEmpty)
    throws XMLStreamException
  {
    final Object value = readValue(reader, allowEmpty);
    if (value != null && !(value instanceof Map))
    {
      throw new XMLStreamException("Properties must be a struct: " + value);
    }
    return (Map) value;
  }

  private static Object readValue(final XMLStreamReader reader, final boolean allowEmpty)
    throws XMLStreamException
  {
    Object output = null;
    reader.require(XMLStreamConstants.START_ELEMENT, null, null);
    final String localName = reader.getLocalName();
    if (localName.equals("struct"))
    {
      final Map struct = new LinkedHashMap();
      while (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
      {
        reader.require(XMLStreamConstants.START_ELEMENT, null, "entry");
        final String name = StaxUtil.getRequiredAttributeValue(reader, null, "name");
        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
        {
          final Object value = readValue(reader, allowEmpty);
          struct.put(name, value);
          reader.nextTag();
        }
        else if (allowEmpty)
        {
          struct.put(name, null);
        }
        reader.require(XMLStreamConstants.END_ELEMENT, null, "entry");
      }
      reader.require(XMLStreamConstants.END_ELEMENT, null, "struct");
      output = struct;
    }
    else if (localName.equals("array"))
    {
      final Collection array = new ArrayList();
      while (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
      {
        reader.require(XMLStreamConstants.START_ELEMENT, null, "element");
        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
        {
          final Object value = readValue(reader, allowEmpty);
          array.add(value);
          reader.nextTag();
        }
        reader.require(XMLStreamConstants.END_ELEMENT, null, "element");
      }
      reader.require(XMLStreamConstants.END_ELEMENT, null, "array");
      output = array;
    }
    else if (localName.equals("string"))
    {
      output = reader.getElementText().trim();
    }
    else if (localName.equals("int"))
    {
      output = NumberUtils.createInteger(reader.getElementText());
    }
    else if (localName.equals("real"))
    {
      output = NumberUtils.createDouble(reader.getElementText());
    }
    else if (localName.equals("bool"))
    {
      output = BooleanUtils.toBooleanObject(reader.getElementText(), "1", "0", null);
    }
    else
    {
      throw new XMLStreamException("Unexpected element " + localName);
    }
    if (output == null)
    {
      throw new XMLStreamException("Unable to parse value");
    }
    return output;
  }

  public static void writeChangeset(
    final OutputStream os,
    final Collection<Map> changedItems,
    final Collection<String> deletedItemIds)
  {
    try
    {
      final XMLStreamWriter writer =
        StaxUtil.newOutputFactory().createXMLStreamWriter(os);
      writer.writeStartDocument();
      writer.writeStartElement("changeset");
      for (final Map properties : changedItems)
      {
        final String id = (String) properties.get("id");
        writeSetItem(writer, id, properties);
      }
      for (final String id : deletedItemIds)
      {
        writeRemoveItem(writer, id);
      }
      writer.writeEndElement();
      writer.writeEndDocument();
      writer.flush();
    }
    catch (final XMLStreamException e)
    {
      throw new RuntimeException("Problem serializing changeset", e);
    }
  }

  public static void writeSetItem(final XMLStreamWriter writer, final String id,
    final Map properties)
    throws XMLStreamException
  {
    writer.writeStartElement("set-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(writer, id, properties, false);
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public static void writeAddToItem(final XMLStreamWriter writer, final String id,
    final Map properties)
    throws XMLStreamException
  {
    writer.writeStartElement("add-to-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(writer, id, properties, false);
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public static void writeRemoveFromItem(final XMLStreamWriter writer, final String id,
    final Map properties)
    throws XMLStreamException
  {
    writer.writeStartElement("remove-from-item");
    writer.writeAttribute("id", id);
    writer.writeStartElement("properties");
    writeValue(writer, id, properties, true);
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public static void writeRemoveAllFromItem(final XMLStreamWriter writer, final String id)
    throws XMLStreamException
  {
    writer.writeStartElement("remove-from-item");
    writer.writeAttribute("id", id);
    writer.writeEmptyElement("all");
    writer.writeEndElement();
  }

  public static void writeAddItem(final XMLStreamWriter writer, final String id)
    throws XMLStreamException
  {
    writer.writeStartElement("add-item");
    writer.writeAttribute("id", id);
    writer.writeEndElement();
  }

  public static void writeRemoveItem(final XMLStreamWriter writer, final String id)
    throws XMLStreamException
  {
    writer.writeStartElement("remove-item");
    writer.writeAttribute("id", id);
    writer.writeEndElement();
  }

  private static void writeValue(final XMLStreamWriter writer, final String id,
    final Object object, final boolean allowEmpty)
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
          writeValue(writer, id + ":" + name, value, allowEmpty);
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
        writeValue(writer, id, value, allowEmpty);
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

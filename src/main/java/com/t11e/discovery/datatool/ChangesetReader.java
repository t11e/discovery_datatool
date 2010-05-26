package com.t11e.discovery.datatool;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * Utility class to use StAX to read/write changesets.
 */
public class ChangesetReader
  extends StaxUtil
{
  private ChangesetReader()
  {
    // Prevent instantiation
  }

  public static void parseChangeset(
    final InputStream is,
    final IItemChangesetListener listener)
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

  public static void parseChangeset(
    final XMLStreamReader reader,
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

  public static void parseChangesetAction(
    final XMLStreamReader reader,
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
          final Map<String,Object> properties = readProperties(reader, false);
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
          final Map<String, Object> properties = readProperties(reader, false);
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
            final Map<String, Object> properties = readProperties(reader, true);
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

  @SuppressWarnings("unchecked")
  private static <T> Map<String, T> readProperties(
    final XMLStreamReader reader,
    final boolean allowEmpty)
    throws XMLStreamException
  {
    final Object value = readValue(reader, allowEmpty);
    if (value != null && !(value instanceof Map< ?, ?>))
    {
      throw new XMLStreamException("Properties must be a struct: " + value);
    }
    return (Map<String, T>) value;
  }

  private static Object readValue(
    final XMLStreamReader reader,
    final boolean allowEmpty)
    throws XMLStreamException
  {
    Object output = null;
    reader.require(XMLStreamConstants.START_ELEMENT, null, null);
    final String localName = reader.getLocalName();
    if (localName.equals("struct"))
    {
      final Map<String, Object> struct = new LinkedHashMap<String, Object>();
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
      final Collection<Object> array = new ArrayList<Object>();
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
    else if (localName.equals("long"))
    {
      output = NumberUtils.createLong(reader.getElementText());
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
}

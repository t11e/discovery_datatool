package com.t11e.discovery.datatool;

import java.io.EOFException;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JsonUtil
{
  private static final NumberFormat s_longNumberFormat = newLongNumberFormat();
  private static NumberFormat newLongNumberFormat()
  {
    final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setGroupingUsed(false);
    return nf;
  }
  private static final NumberFormat s_doubleNumberFormat = newDoubleNumberFormat();
  private static NumberFormat newDoubleNumberFormat()
  {
    final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setGroupingUsed(false);
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(30);
    return nf;
  }

  public static String encode(final Object obj)
  {
    final StringWriter sw = new StringWriter();
    try
    {
      encode(sw, obj);
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  @SuppressWarnings("unchecked")
  public static void encode(final Writer writer, final Object obj)
    throws IOException
  {
    if (obj == null)
    {
      encodeNull(writer);
    }
    else if (obj instanceof Boolean)
    {
      encodeBoolean(writer, ((Boolean) obj).booleanValue());
    }
    else if (obj instanceof Integer || obj instanceof Long)
    {
      encodeLong(writer, (Number) obj);
    }
    else if (obj instanceof Double || obj instanceof Float)
    {
      encodeDouble(writer, (Number) obj);
    }
    else if (obj instanceof String)
    {
      encodeString(writer, (String) obj);
    }
    else if (obj instanceof Collection)
    {
      encodeCollection(writer, (Collection) obj);
    }
    else if (obj.getClass().isArray())
    {
      encodeArray(writer, obj);
    }
    else if (obj instanceof Map)
    {
      encodeMap(writer, (Map) obj);
    }
    else
    {
      throw new IllegalArgumentException("Unsupported object type: " +
        obj.getClass().getName());
    }
  }

  public static void encodeNull(final Writer writer)
    throws IOException
  {
    writer.write("null");
  }

  public static void encodeBoolean(final Writer writer, final boolean value)
    throws IOException
  {
    writer.write(value ? "true" : "false");
  }

  public static void encodeLong(final Writer writer, final Number value)
    throws IOException
  {
    String serialized;
    synchronized (s_longNumberFormat)
    {
      serialized = s_longNumberFormat.format(value.longValue());
    }
    writer.write(serialized);
  }

  public static void encodeDouble(final Writer writer, final Number value)
    throws IOException
  {
    String serialized;
    synchronized (s_doubleNumberFormat)
    {
      serialized = s_doubleNumberFormat.format(value.doubleValue());
    }
    writer.write(serialized);
  }

  public static void encodeString(final Writer writer, final String value)
    throws IOException
  {
    writer.write("\"");
    for (int i = 0; i < value.length(); i++)
    {
      final char c = value.charAt(i);
      switch (c)
      {
        case '\\':
          writer.write("\\\\");
          break;
        case '"':
          writer.write("\\\"");
          break;
        case '\b':
          writer.write("\\b");
          break;
        case '\f':
          writer.write("\\f");
          break;
        case '\n':
          writer.write("\\n");
          break;
        case '\r':
          writer.write("\\r");
          break;
        case '\t':
          writer.write("\\t");
          break;
        default:
          // Magic values from JSON specification (RFC 4627).
          if (c < 0x20 || c == 0x24 || c == 0x5C)
          {
            writer.write("\\u");
            writer.write(asHexString(c));
          }
          else
          {
            writer.write(c);
          }
          break;
      }
    }
    writer.write("\"");
  }

  @SuppressWarnings("unchecked")
  public static void encodeCollection(final Writer writer, final Collection collection)
    throws IOException
  {
    writer.write("[");
    int count = 0;
    for (final Iterator i = collection.iterator(); i.hasNext(); count++)
    {
      final Object elem = i.next();
      if (count > 0)
      {
        writer.write(",");
      }
      encode(writer, elem);
    }
    writer.write("]");
  }

  public static void encodeArray(final Writer writer, final Object array)
    throws IOException
  {
    writer.write("[");
    final int N = Array.getLength(array);
    for (int i = 0; i < N; i++)
    {
      final Object elem = Array.get(array, i);
      if (i > 0)
      {
        writer.write(",");
      }
      try
      {
        encode(writer, elem);
      }
      catch (final IllegalArgumentException e)
      {
        throw new IllegalArgumentException("Problem encoding Array of " +
          array.getClass().getComponentType() + ": " + e.getMessage());
      }
    }
    writer.write("]");
  }

  @SuppressWarnings("unchecked")
  public static void encodeMap(final Writer writer, final Map map)
    throws IOException
  {
    writer.write("{");
    int count = 0;
    final List keys = new ArrayList(map.keySet());
    Collections.sort(keys);
    for (final Iterator i = keys.iterator(); i.hasNext(); count++)
    {
      final String key = (String) i.next();
      final Object value = map.get(key);
      if (count > 0)
      {
        writer.write(",");
      }
      encode(writer, key);
      writer.write(":");
      try
      {
        encode(writer, value);
      }
      catch (final RuntimeException e)
      {
        throw new RuntimeException("Problem encoding map entry " + key, e);
      }
    }
    writer.write("}");
  }

  public static Object decode(final String json)
  {
    Object output;
    try
    {
      output = decode(new PushbackReader(new StringReader(json)));
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
    return output;
  }

  public static Object decode(final PushbackReader reader)
    throws IOException
  {
    Object output;
    skipWhiteSpace(reader);
    final int c = reader.read();
    switch (c)
    {
      case '[':
        reader.unread(c);
        output = decodeArray(reader);
        break;
      case ']':
        throw new JsonParseException("Unexpected ]");
      case '{':
        reader.unread(c);
        output = decodeObject(reader);
        break;
      case '}':
        throw new JsonParseException("Unexpected }");
      case '"':
        reader.unread(c);
        output = decodeString(reader);
        break;
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
      case '-':
        reader.unread(c);
        output = decodeNumber(reader);
        break;
      case 'f':
        eatToken(reader, "false", 1);
        output = Boolean.FALSE;
        break;
      case 't':
        eatToken(reader, "true", 1);
        output = Boolean.TRUE;
        break;
      case 'n':
        eatToken(reader, "null", 1);
        output = null;
        break;
      case -1:
        throw new EOFException();
      default:
        throw new JsonParseException("Unexpected character '" + (char) c + "' (code " + c + ")");
    }
    return output;
  }

  @SuppressWarnings("unchecked")
  public static Collection decodeArray(final PushbackReader reader)
    throws IOException
  {
    skipWhiteSpace(reader);
    int c = reader.read();
    if (c != '[')
    {
      throw new JsonParseException("Expected [ at start of array");
    }
    Collection output = null;
    Collection array = null;
    while (output == null)
    {
      skipWhiteSpace(reader);
      c = reader.read();
      switch (c)
      {
        case ',':
          if (array == null)
          {
            throw new JsonParseException("Unexpected , before first array element");
          }
          skipWhiteSpace(reader);
          break;
        case ']':
          output = array == null ? Collections.EMPTY_LIST : array;
          break;
        case -1:
          throw new JsonParseException("Premature end of array");
        default:
          if (array != null)
          {
            throw new JsonParseException("Missing , in array");
          }
          reader.unread(c);
          break;
      }
      if (output == null)
      {
        final Object element = decode(reader);
        if (array == null)
        {
          array = new ArrayList();
        }
        array.add(element);
      }
    }
    return output;
  }

  @SuppressWarnings("unchecked")
  public static Map decodeObject(final PushbackReader reader)
    throws IOException
  {
    skipWhiteSpace(reader);
    int c = reader.read();
    if (c != '{')
    {
      throw new JsonParseException("Expected [ at start of object");
    }
    Map output = null;
    Map map = null;
    while (output == null)
    {
      skipWhiteSpace(reader);
      c = reader.read();
      switch (c)
      {
        case ',':
          if (map == null)
          {
            throw new JsonParseException("Unexpected , before first object element");
          }
          skipWhiteSpace(reader);
          break;
        case '}':
          output = map == null ? Collections.EMPTY_MAP : map;
          break;
        case -1:
          throw new JsonParseException("Premature end of object");
        default:
          if (map != null)
          {
            throw new JsonParseException("Missing , in object");
          }
          reader.unread(c);
          break;
      }
      if (output == null)
      {
        Object value = decode(reader);
        if (!(value instanceof String))
        {
          throw new JsonParseException("Found non string key for object '" + value + "'");
        }
        final String key = (String) value;
        skipWhiteSpace(reader);
        c = reader.read();
        if (c != ':')
        {
          throw new JsonParseException("Missing : in object");
        }
        skipWhiteSpace(reader);
        value = decode(reader);
        if (map == null)
        {
          map = new HashMap();
        }
        map.put(key, value);
      }
    }
    return output;
  }

  public static String decodeString(final PushbackReader reader)
    throws IOException
  {
    skipWhiteSpace(reader);
    int c = reader.read();
    if (c != '"')
    {
      throw new JsonParseException("Expected [ at start of string");
    }
    String output = null;
    final StringBuffer buffer = new StringBuffer();
    do
    {
      c = reader.read();
      switch (c)
      {
        case '\\':
          c = reader.read();
          switch (c)
          {
            case '"':
            case '\\':
            case '/':
              buffer.append((char) c);
              break;
           case 'b':
              buffer.append('\b');
              break;
            case 'f':
              buffer.append('\f');
              break;
            case 'n':
              buffer.append('\n');
              break;
            case 'r':
              buffer.append('\r');
              break;
            case 't':
              buffer.append('\t');
              break;
            case 'u':
            {
              char escaped = 0;
              for (int i = 3; i >= 0; i--)
              {
                c = reader.read();
                if (c == -1)
                {
                  throw new JsonParseException("Premature end of unicode escape");
                }
                switch (c)
                {
                  case '0': c = 0; break;
                  case '1': c = 1; break;
                  case '2': c = 2; break;
                  case '3': c = 3; break;
                  case '4': c = 4; break;
                  case '5': c = 5; break;
                  case '6': c = 6; break;
                  case '7': c = 7; break;
                  case '8': c = 8; break;
                  case '9': c = 9; break;
                  case 'a': c = 10; break;
                  case 'A': c = 10; break;
                  case 'b': c = 11; break;
                  case 'B': c = 11; break;
                  case 'c': c = 12; break;
                  case 'C': c = 12; break;
                  case 'd': c = 13; break;
                  case 'D': c = 13; break;
                  case 'e': c = 14; break;
                  case 'E': c = 14; break;
                  case 'f': c = 15; break;
                  case 'F': c = 15; break;
                  default: throw new JsonParseException("Invalid hex digit in unicode escape");
                }
                escaped |= c << (4 * i);
              }
              buffer.append(escaped);
              break;
            }
            default:
              throw new JsonParseException("Unexpected escape character '" +
                (char) c + "'");
          }
          break;
        case '"':
          output = buffer.toString();
          break;
        case -1:
          throw new JsonParseException("Premature end of string");
        default:
          buffer.append((char) c);
          break;
      }
    }
    while (output == null);
    return output;
  }

  public static Number decodeNumber(final PushbackReader reader)
    throws IOException
  {
    skipWhiteSpace(reader);
    String number = null;
    boolean isDouble = false;
    {
      final StringBuffer buffer = new StringBuffer();
      while (number == null)
      {
        final int c = reader.read();
        switch (c)
        {
          case '.': case 'e': case 'E':
            isDouble = true;
            break;
          case '+': case '-':
            if (!isDouble && !(c == '-' && buffer.length() == 0))
            {
              throw new JsonParseException("Unexpected " + (char) c +
                " in number");
            }
            break;
          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9':
            break;
          default:
            reader.unread(c);
            number = buffer.toString();
            break;
        }
        buffer.append((char) c);
      }
    }
    Number output = null;
    if (isDouble)
    {
      output = Double.valueOf(number);
    }
    else
    {
      final long v = Long.parseLong(number);
      if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
      {
        output = new Integer((int) v);
      }
      else
      {
        output = new Long(v);
      }
    }
    return output;
  }

  public static class JsonParseException
    extends RuntimeException
  {
    private static final long serialVersionUID = 2444420457854685005L;

    public JsonParseException(final String message)
    {
      super(message);
    }
  }

  private static void skipWhiteSpace(final PushbackReader reader)
    throws IOException
  {
    int c;
    boolean done = false;
    do
    {
      c = reader.read();
      switch (c)
      {
        case ' ':
        case '\t':
        case '\r':
        case '\n':
          // JSON defined whitespace
          break;
        case -1:
          // EOF
          done = true;
          break;
        default:
          reader.unread(c);
          done = true;
          break;
      }
    }
    while (!done);
  }

  private static void eatToken(final Reader reader, final String token, final int offset)
    throws IOException
  {
    int c;
    for (int i = offset; i < token.length(); i++)
    {
      c = reader.read();
      if (c == -1)
      {
        throw new EOFException("EOF when parsing token '" + token + "'");
      }
      else if (c != token.charAt(i))
      {
        throw new JsonParseException("Unexpected character when parsing token '"
          + token + "'");
      }
    }
  }


  private static final char[] HEX_CHARACTERS_UPPER = new char[] {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'A', 'B', 'C', 'D', 'E', 'F'};

  private static String asHexString(final char c)
  {
    final StringBuffer buffer = new StringBuffer(4);
    asHexString(buffer, c);
    return buffer.toString();
  }

  private static void asHexString(final StringBuffer buffer, final char c)
  {
    buffer.append(HEX_CHARACTERS_UPPER[(c >> 12) & 0xf]);
    buffer.append(HEX_CHARACTERS_UPPER[(c >> 8) & 0xf]);
    buffer.append(HEX_CHARACTERS_UPPER[(c >> 4) & 0xf]);
    buffer.append(HEX_CHARACTERS_UPPER[c & 0xf]);
  }
}

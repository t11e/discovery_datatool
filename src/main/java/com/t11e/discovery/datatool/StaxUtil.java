package com.t11e.discovery.datatool;

import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class StaxUtil
{
  /**
   * Create a new XMLInputFactory, better than calling
   * XMLInputFactory.newInstance() directly as it allows for
   * easier override without dealing with endorsed directories.
   */
  public static XMLInputFactory newInputFactory()
  {
    return new com.ctc.wstx.stax.WstxInputFactory();
  }

  /**
   * Create a new XMLOutputFactory, better than calling
   * XMLOutputFactory() directly as it allows for
   * easier override without dealing with endorsed directories.
   */
  public static XMLOutputFactory newOutputFactory()
  {
    return new com.ctc.wstx.stax.WstxOutputFactory();

  }

  public static String getRequiredAttributeValue(final XMLStreamReader reader,
    final String ns, final String localName)
    throws XMLStreamException
  {
    final String value = reader.getAttributeValue(ns, localName);
    if (value == null)
    {
      throw newMissingAttributeException(ns, localName);
    }
    return value;
  }

  public static XMLStreamException newMissingAttributeException(final String ns,
    final String name)
  {
    return new XMLStreamException("The required attribute " +
      (ns == null ? "" : "{" + ns + "}") + name + " is missing");
  }

  /**
   * Variant of XMLStreamReader.nextTag that additionally ignores any DTD tokens.
   */
  public static int nextTagIgnoringDocType(final XMLStreamReader reader)
    throws XMLStreamException
  {
    int next = -1;
    boolean done = false;
    do
    {
      next = reader.next();
      switch (next)
      {
        case XMLStreamConstants.SPACE:
        case XMLStreamConstants.COMMENT:
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
          break;
        case XMLStreamConstants.CDATA:
        case XMLStreamConstants.CHARACTERS:
          if (!reader.isWhiteSpace())
          {
            throw new XMLStreamException("Received non-all-whitespace CHARACTERS" +
              " or CDATA event in nextTagIgnoringDocType().");
          }
          break;
        case XMLStreamConstants.START_ELEMENT:
        case XMLStreamConstants.END_ELEMENT:
          done = true;
          break;
        case XMLStreamConstants.DTD:
          // Swallow
          break;
        default:
          throw new XMLStreamException("Received event " + tokenTypeDesc(next)
            + ", instead of START_ELEMENT or END_ELEMENT.");
      }
    }
    while (!done);
    return next;
  }

  /**
   * Find either the next START_ELEMENT or text, returns the text if
   * available and null if a tag was found.
   */
  public static String nextTextOrTag(final XMLStreamReader reader)
    throws XMLStreamException
  {
    String output = null;
    reader.require(XMLStreamConstants.START_ELEMENT, null, null);
    StringBuffer buffer = null;
    boolean done = false;
    do
    {
      final int type = reader.next();
      switch (type)
      {
        case XMLStreamConstants.CHARACTERS:
        case XMLStreamConstants.CDATA:
        case XMLStreamConstants.SPACE:
        case XMLStreamConstants.ENTITY_REFERENCE:
          if (buffer == null)
          {
            buffer = new StringBuffer();
          }
          buffer.append(reader.getText());
          break;
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
        case XMLStreamConstants.COMMENT:
          // Swallow
          break;
        case XMLStreamConstants.START_ELEMENT:
          if (buffer != null)
          {
            boolean containsNonWhitespace = false;
            for (int i = 0; i < buffer.length(); i++)
            {
              if (!Character.isWhitespace(buffer.charAt(i)))
              {
                containsNonWhitespace = true;
                break;
              }
            }
            if (containsNonWhitespace)
            {
              throw new XMLStreamException("Mixed text and elements when " +
                " looking for the next text or tag" +
                "; tag=" + reader.getLocalName() +
                " text='" + buffer.toString() + "'");
            }
          }
          done = true;
          break;
        case XMLStreamConstants.END_ELEMENT:
          output = (buffer == null) ? "" : buffer.toString();
          done = true;
          break;
        default:
          throw new XMLStreamException("Unexpected event " +
            tokenTypeDesc(type) + " when looking for text or tag");
      }
    }
    while (!done);
    if (output == null)
    {
      reader.require(XMLStreamConstants.START_ELEMENT, null, null);
    }
    else
    {
      reader.require(XMLStreamConstants.END_ELEMENT, null, null);
    }
    return output;
  }

  public static void skipNestedElements(final XMLStreamReader reader)
    throws XMLStreamException
  {
    int level = 0;
    while (level >= 0)
    {
      final int next = reader.next();
      switch (next)
      {
        case XMLStreamConstants.SPACE:
        case XMLStreamConstants.COMMENT:
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
        case XMLStreamConstants.CDATA:
        case XMLStreamConstants.CHARACTERS:
          break;
        case XMLStreamConstants.START_ELEMENT:
          level++;
          break;
        case XMLStreamConstants.END_ELEMENT:
          level--;
          break;
        default:
          throw new XMLStreamException("Received event " + tokenTypeDesc(next)
            + ", instead of START_ELEMENT or END_ELEMENT.");
      }
    }
  }

  protected static String tokenTypeDesc(final int type)
  {
    String desc;
    switch (type)
    {
      case XMLStreamConstants.START_ELEMENT:
        desc = "START_ELEMENT";
        break;
      case XMLStreamConstants.END_ELEMENT:
        desc = "END_ELEMENT";
        break;
      case XMLStreamConstants.START_DOCUMENT:
        desc = "START_DOCUMENT";
        break;
      case XMLStreamConstants.END_DOCUMENT:
        desc = "END_DOCUMENT";
        break;
      case XMLStreamConstants.CHARACTERS:
        desc = "CHARACTERS";
        break;
      case XMLStreamConstants.CDATA:
        desc = "CDATA";
        break;
      case XMLStreamConstants.SPACE:
        desc = "SPACE";
        break;
      case XMLStreamConstants.COMMENT:
        desc = "COMMENT";
        break;
      case XMLStreamConstants.PROCESSING_INSTRUCTION:
        desc = "PROCESSING_INSTRUCTION";
        break;
      case XMLStreamConstants.DTD:
        desc = "DTD";
        break;
      case XMLStreamConstants.ENTITY_REFERENCE:
        desc = "ENTITY_REFERENCE";
        break;
      default:
        desc = "UNKNOWN_" + type;
    }
    return desc;
  }

  public static void writeCharactersIfNotNull(final XMLStreamWriter writer,
    final String chars)
    throws XMLStreamException
  {
    if (chars != null)
    {
      writer.writeCharacters(chars);
    }
  }

  public static void writeAttributeIfNotNull(final XMLStreamWriter writer,
    final String name, final String value)
    throws XMLStreamException
  {
    if (value != null)
    {
      writer.writeAttribute(name, value);
    }
  }

  private static final Pattern INVALID_UTF_CHARS =
      Pattern.compile("[^" +
        "\\u0009" +
        "\\u000A" +
        "\\u000D" +
        "\\u0020-\\uD7FF" +
        "\\uE000-\\uFFFF" +
        "]");

  public static String filterInvalidCharacters(final String content)
  {
    final String filtered = INVALID_UTF_CHARS.matcher(content).replaceAll("");
    return filtered;
  }

  /**
   * Call handler.characters filtering any invalid UTF characters.
   *
   * The XML Specification lists the following valid UTF characters.
   *   http://www.w3.org/TR/REC-xml/#charsets
   *
   * <pre>
   *   Char       ::=          #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
   *     any Unicode character, excluding the surrogate blocks, FFFE, and FFFF.
   * </pre>
   *
   * Document authors are encouraged to avoid "compatibility characters",
   * as defined in section 2.3 of [Unicode]. The characters defined in the
   * following ranges are also discouraged. They are either control characters
   * or permanently undefined Unicode characters:
   *
   * <pre>
   *   [#x7F-#x84], [#x86-#x9F], [#xFDD0-#xFDEF],
   *   [#x1FFFE-#x1FFFF], [#x2FFFE-#x2FFFF], [#x3FFFE-#x3FFFF],
   *   [#x4FFFE-#x4FFFF], [#x5FFFE-#x5FFFF], [#x6FFFE-#x6FFFF],
   *   [#x7FFFE-#x7FFFF], [#x8FFFE-#x8FFFF], [#x9FFFE-#x9FFFF],
   *   [#xAFFFE-#xAFFFF], [#xBFFFE-#xBFFFF], [#xCFFFE-#xCFFFF],
   *   [#xDFFFE-#xDFFFF], [#xEFFFE-#xEFFFF], [#xFFFFE-#xFFFFF],
   *   [#x10FFFE-#x10FFFF].
   * </pre>
   * @throws XMLStreamException
   **/
  public static void writeFilteredCharacters(
    final XMLStreamWriter writer,
    final String characters)
    throws XMLStreamException
  {
    if (characters != null)
    {
      final String filtered = filterInvalidCharacters(characters);
      writer.writeCharacters(filtered);
    }
  }

  protected StaxUtil()
  {
    // Prevent instantiation
  }
}

package com.t11e.discovery.datatool;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class DataToolEntityResolver
  implements EntityResolver
{
  @Override
  public InputSource resolveEntity(final String publicId, final String systemId)
    throws SAXException, IOException
  {
    InputSource result = null;
    if (publicId == null && systemId != null &&
      systemId.startsWith("http://transparensee.com/schema/"))
    {
      final String resourceName =
        StringUtils.removeStart(systemId, "http://transparensee.com/schema/");
      if (resourceName.contains("/"))
      {
        throw new RuntimeException("Unsupported XML entity " + publicId + " " + systemId);
      }
      final InputStream is = getClass().getResourceAsStream("/" + resourceName);
      if (is == null)
      {
        throw new RuntimeException("Unable to find resource /" + resourceName);
      }
      result = new InputSource(is);
    }
    else
    {
      throw new RuntimeException("Unsupported XML entity " + publicId + " " + systemId);
    }
    return result;
  }
}
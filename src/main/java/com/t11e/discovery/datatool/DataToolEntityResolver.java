/**
 *
 */
package com.t11e.discovery.datatool;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class DataToolEntityResolver
  implements EntityResolver
{
  public InputSource resolveEntity(final String publicId, final String systemId)
    throws SAXException, IOException
  {
    InputSource result = null;
    if ("datatool-config-1".equals(publicId))
    {
      final InputStream is = ConfigurationManager.class
        .getResourceAsStream("/datatool-config-1.dtd");
      if (is == null)
      {
        throw new RuntimeException("Unable to find discovery_datatool_config.dtd");
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
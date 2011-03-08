package com.t11e.discovery.datatool;

import java.io.InputStream;

public class IntegrationTest3
  extends IntegrationTestBase
{
  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("IntegrationTest-3.xml");
  }
}

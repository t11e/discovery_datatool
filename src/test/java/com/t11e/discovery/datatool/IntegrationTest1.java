package com.t11e.discovery.datatool;

import java.io.InputStream;

public class IntegrationTest1
  extends IntegrationTestBase
{
  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("IntegrationTest-1.xml");
  }
}

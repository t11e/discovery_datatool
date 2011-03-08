package com.t11e.discovery.datatool;

import java.io.InputStream;

public class IntegrationTest4
  extends IntegrationTestBase
{
  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("IntegrationTest-4.xml");
  }
}

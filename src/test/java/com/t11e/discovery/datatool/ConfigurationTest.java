package com.t11e.discovery.datatool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.XMLPropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTest
{
  @Test
  public void testLoadPropertiesFile()
    throws ConfigurationException
  {
    final Configuration config =
      new PropertiesConfiguration("src/config/configuration-test.properties");
    testConfig(config);
  }

  @Test
  public void testLoadPropertiesFile2()
    throws ConfigurationException
  {
    final Configuration config =
      new PropertiesConfiguration("src/config/configuration-test.properties");
    testConfig2(ConfigurationUtils.convertToHierarchical(config));
  }

  @Test
  public void testLoadIniFile()
    throws ConfigurationException
  {
    final Configuration config =
      new PropertiesConfiguration("src/config/configuration-test.ini");
    testConfig(config);
  }

  @Test
  public void testLoadIniFile2()
    throws ConfigurationException
  {
    final HierarchicalConfiguration config =
      new HierarchicalINIConfiguration("src/config/configuration-test.ini");
    testConfig2(config);
  }

  @Test
  public void testLoadXmlFile()
    throws ConfigurationException
  {
    final Configuration config =
      new XMLPropertiesConfiguration("src/config/configuration-test.xml");
    testConfig(config);
  }

  @Test
  public void testLoadXmlFile2()
    throws ConfigurationException
  {
    final HierarchicalConfiguration config =
      new XMLConfiguration("src/config/configuration-test.xml");
    testConfig2(config);
  }

  //
  // Flat config
  //

  private void testConfig(final Configuration config)
  {
    ConfigurationUtils.dump(config, System.err);
    Assert.assertEquals(
      CollectionsFactory.makeList("mysqlTest"),
      getChildKeys(config, "datasource"));
    testDataSourceConfig(config.subset("datasource.mysqlTest"));
  }


  private void testDataSourceConfig(final Configuration config)
  {
    ConfigurationUtils.dump(config, System.err);
    Assert.assertEquals("jdbc:mysql://localhost/test", config.getString("url"));
    Assert.assertEquals("test", config.getString("user"));
    Assert.assertEquals("test", config.getString("password"));
  }

  @SuppressWarnings("unchecked")
  private List<String> getChildKeys(final Configuration config, final String prefix)
  {
    final List<String> result = new ArrayList<String>();
    for(final Iterator<String> i = config.getKeys(prefix); i.hasNext(); )
    {
      final String t = i.next();
      System.err.println("-" + t);
      final String key = t.substring(prefix.length());
      if (key.length() > 0 && key.charAt(0) == '.')
      {
        final String child = StringUtils.substringBefore(key.substring(1), ".");
        if (StringUtils.isNotBlank(child) && !result.contains(child))
        {
          result.add(child);
        }
      }
    }
    return result;
  }

  //
  // Hierarchical config
  //

  @SuppressWarnings("unchecked")
  private void testConfig2(final HierarchicalConfiguration config)
  {
    final List configs = config.configurationsAt("datasource.*");
    Assert.assertEquals(1, configs.size());
  }
}

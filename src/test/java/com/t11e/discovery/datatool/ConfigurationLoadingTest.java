package com.t11e.discovery.datatool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = {"applicationContext-test.xml"})
public class ConfigurationLoadingTest
{
  @Autowired
  private ConfigurationManager configurationManager;

  @Test
  public void testMinimalConfiguration()
    throws IOException
  {
    configurationManager.loadConfiguration(
      IOUtils.toInputStream(
            "<?xml version='1.0' encoding='utf-8'?>" +
              "<config xmlns='http://transparensee.com/schema/datatool-config-4' " +
              "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
              "xsi:schemaLocation='http://transparensee.com/schema/datatool-config-4 " +
              "http://transparensee.com/schema/datatool-config-4.xsd'>\n" +
              "  <dataSources>\n" +
              "    <driver name='dataSource' class='org.apache.derby.jdbc.EmbeddedDriver'>\n" +
              "      <url>jdbc:derby:memory:test;create=true</url>\n" +
              "    </driver>\n" +
              "  </dataSources>\n" +
              "</config>",
        "utf-8"),
      false);
  }

  @Test
  public void testFileConfigurations()
    throws Exception
  {
    @SuppressWarnings("unchecked")
    final ArrayList<File> validConfigFiles = new ArrayList<File>(
        FileUtils.listFiles(new File("src/test/java/com/t11e/discovery/datatool/configuration_loading"),
          FileFilterUtils.suffixFileFilter("-valid.xml"),
          FileFilterUtils.prefixFileFilter("v")));
    Assert.assertFalse(validConfigFiles.isEmpty());

    @SuppressWarnings("unchecked")
    final ArrayList<File> invalidConfigFiles = new ArrayList<File>(
        FileUtils.listFiles(new File("src/test/java/com/t11e/discovery/datatool/configuration_loading"),
          FileFilterUtils.suffixFileFilter("-invalid.xml"),
          FileFilterUtils.prefixFileFilter("v")));
    Assert.assertFalse(invalidConfigFiles.isEmpty());

    final Collection<String> failures = new ArrayList<String>();
    for (final File config : validConfigFiles)
    {
      try
      {
        configurationManager.loadConfiguration(FileUtils.openInputStream(config), false);
        System.out.println("Passed as   valid: " + config.getParentFile().getName() + "/" + config.getName());
      }
      catch (final RuntimeException e)
      {
        failures.add("Problem loading " + config.getParentFile().getName() + "/" + config.getName() + ": "
          + e.getMessage());
      }
    }
    for (final File config : invalidConfigFiles)
    {
      try
      {
        configurationManager.loadConfiguration(FileUtils.openInputStream(config), false);
        failures.add("Should have not loaded " + config.getParentFile().getName() + "/" + config.getName());
      }
      catch (final RuntimeException e)
      {
        System.out.println("Passed as invalid: " + config.getParentFile().getName() + "/" + config.getName());
        // success
      }
    }
    if (!failures.isEmpty())
    {
      Assert.fail(StringUtils.join(failures, "\n"));
    }
  }
}

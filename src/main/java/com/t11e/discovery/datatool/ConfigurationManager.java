package com.t11e.discovery.datatool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component("ConfigurationManager")
public class ConfigurationManager
{
  private File configurationFile = new File("discovery_datatool.xml");
  private boolean exitOnInvalidConfigAtStartup;
  private ConfigurableApplicationContext currentContext;

  @PostConstruct
  public void onPostConstruct()
  {
    try
    {
      loadConfiguration(new FileInputStream(configurationFile), false);
    }
    catch (final RuntimeException e)
    {
      exitOnStartupIfConfigured(e);
      throw e;
    }
    catch (final FileNotFoundException e)
    {
      exitOnStartupIfConfigured(e);
      // Otherwise ignore this error
    }
  }

  // TODO This is a hack so we don't need to dive into the Jetty/Spring
  // lifecycle stuff. It allows us to terminate the tool when running it from
  // a standalone jar if the config is missing or invalid.
  private void exitOnStartupIfConfigured(final Throwable e)
  {
    if (exitOnInvalidConfigAtStartup)
    {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  public boolean loadConfiguration(
    final InputStream is,
    final boolean persist)
  {
    boolean persisted = false;
    byte[] config;
    try
    {
      config = IOUtils.toByteArray(is);
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
    IOUtils.closeQuietly(is);
    final GenericApplicationContext newContext = createApplicationContext(new ByteArrayInputStream(config));
    newContext.start();

    synchronized(this)
    {
      if (persist)
      {
        persisted = swapConfigFiles(config);
      }
      if (currentContext != null)
      {
        currentContext.stop();
        currentContext.close();
        currentContext = null;
      }
      currentContext = newContext;
    }
    return persisted;
  }

  private boolean swapConfigFiles(final byte[] config)
  {
    boolean changed = false;
    try
    {
      final File newConfig =
        File.createTempFile(configurationFile.getName(), ".tmp",
          configurationFile.getCanonicalFile().getParentFile());
      FileUtils.writeByteArrayToFile(newConfig, config);
      final File backupFile = new File(configurationFile + ".bak");
      if (configurationFile.exists())
      {
        try
        {
          FileUtils.forceDelete(backupFile);
        }
        catch (final FileNotFoundException e)
        {
          // Ignore
        }
        FileUtils.moveFile(configurationFile, backupFile);
      }
      FileUtils.moveFile(newConfig, configurationFile);
      FileUtils.deleteQuietly(backupFile);
      changed = true;
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
    return changed;
  }

  public <T> T getBean(final Class<T> klass)
    throws BeansException
  {
    synchronized (this)
    {
      return currentContext.getBean(klass);
    }
  }

  @SuppressWarnings("unchecked")
  private static GenericApplicationContext createApplicationContext(final InputStream is)
  {
    final GenericApplicationContext applicationContext = new GenericApplicationContext();

    final SAXReader saxReader = new SAXReader(true);
    try
    {
      saxReader.setFeature("http://apache.org/xml/features/validation/schema", true);
    }
    catch (final SAXException e)
    {
      throw new RuntimeException(e);
    }
    saxReader.setEntityResolver(new DataToolEntityResolver());
    {
      final DocumentFactory factory = new DocumentFactory();
      factory.setXPathNamespaceURIs(CollectionsFactory.makeMap(
        "c", "http://transparensee.com/schema/datatool-config-1"));
      saxReader.setDocumentFactory(factory);
    }
    final Document document;
    try
    {
      document = saxReader.read(is);
    }
    catch (final DocumentException e)
    {
      throw new RuntimeException(e);
    }
    {
      for (final Node node : (List<Node>) document.selectNodes("/c:config/c:dataSources/c:dataSource"))
      {
        final String name = node.valueOf("@name");
        final Class<DataSource> clazz = loadDataSource(node.valueOf("@class"), node.valueOf("@jar"));
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        for (final Node child : (List<Node>) node.selectNodes("*"))
        {
          builder.addPropertyValue(
            child.getName(), StringUtils.trimToEmpty(child.getText()));
        }
        applicationContext.registerBeanDefinition("dataSource-" + name, builder.getBeanDefinition());
      }
    }
    {
      for (final Node node : (List<Node>) document.selectNodes("/c:config/c:profiles/c:sqlProfile"))
      {
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlChangesetProfileService.class);
        builder.addPropertyReference("dataSource", "dataSource-" + node.valueOf("@dataSource"));
        builder.addPropertyValue("retrieveStartColumn", node.valueOf("c:retrieveSql/@startColumn"));
        builder.addPropertyValue("retrieveEndColumn", node.valueOf("c:retrieveSql/@endColumn"));
        builder.addPropertyValue("retrieveSql", node.valueOf("c:retrieveSql/text()"));
        builder.addPropertyValue("updateSql", node.valueOf("c:updateSql/text()"));
        applicationContext.registerBeanDefinition("profile-" + node.valueOf("@name"), builder.getBeanDefinition());
      }
    }
    {
      final List<ChangesetPublisher> publishers = new ArrayList<ChangesetPublisher>();
      for (final Node node : (List<Node>) document.selectNodes("/c:config/c:publishers/c:sqlPublisher"))
      {
        final List<SqlAction> actions = new ArrayList<SqlAction>();
        for (final Node action : (List<Node>) node.selectNodes("c:action"))
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlAction.class);
          builder.addPropertyValue("action", action.valueOf("@type"));
          builder.addPropertyValue("filter", action.valueOf("@filter"));
          builder.addPropertyValue("query", StringUtils.trimToEmpty(action.valueOf("c:query/text()")));
          builder.addPropertyValue("idColumn", action.valueOf("@idColumn"));
          builder.addPropertyValue("jsonColumnNames", action.valueOf("@jsonColumnNames"));
          final String beanName = "SqlAction-" + System.identityHashCode(builder);
          applicationContext.registerBeanDefinition(beanName, builder.getBeanDefinition());
          actions.add(applicationContext.getBean(beanName, SqlAction.class));
        }
        BeanDefinition sqlChangesetExtractor;
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlChangesetExtractor.class);
          builder.addPropertyReference("dataSource", "dataSource-" + node.valueOf("@dataSource"));
          builder.addPropertyValue("actions", actions);
          sqlChangesetExtractor = builder.getBeanDefinition();
        }
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ChangesetPublisher.class);
          final String name = node.valueOf("@name");
          builder.addPropertyValue("name", name);
          builder.addPropertyReference("changesetProfileService", "profile-" + node.valueOf("@profile"));
          builder.addPropertyValue("changesetExtractor", sqlChangesetExtractor);
          final String beanName = "Publisher-" + name;
          applicationContext.registerBeanDefinition(beanName, builder.getBeanDefinition());
          publishers.add(applicationContext.getBean(beanName, ChangesetPublisher.class));
        }
      }
      {
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ChangesetPublisherManager.class);
        builder.addPropertyValue("publishers", publishers);
        applicationContext.registerBeanDefinition("ChangesetPublisherManager", builder.getBeanDefinition());
      }
    }
    applicationContext.refresh();
    return applicationContext;
  }

  private static Class<DataSource> loadDataSource(
    final String dataSourceClassName,
    final String jarPath)
  {
    try
    {
      final URL jarUrl = new File(jarPath).toURI().toURL();
      return loadDataSource(dataSourceClassName, jarUrl);
    }
    catch (final MalformedURLException e)
    {
      throw new RuntimeException(e);
    }
  }

    @SuppressWarnings("unchecked")
  private static Class<DataSource> loadDataSource(
    final String dataSourceClassName,
    final URL jarUrl)
  {
    Class<DataSource> driverClass;
    try
    {
      if (jarUrl == null)
      {
        driverClass = (Class<DataSource>) Class.forName(dataSourceClassName);
      }
      else
      {
        final URLClassLoader classLoader = new URLClassLoader(new URL[] {jarUrl});
        driverClass = (Class<DataSource>) classLoader.loadClass(dataSourceClassName);
      }
    }
    catch (final ClassCastException e)
    {
      throw new RuntimeException(dataSourceClassName + " is not a DataSource, from " + jarUrl, e);
    }
    catch (final ClassNotFoundException e)
    {
      throw new RuntimeException("Could not find the DataSource: " + dataSourceClassName + " from " + jarUrl, e);
    }
    return driverClass;
  }

  public void setExitOnInvalidConfigAtStartup(
    final boolean exitOnInvalidConfigAtStartup)
  {
    this.exitOnInvalidConfigAtStartup = exitOnInvalidConfigAtStartup;
  }
  public void setConfigurationFile(final String configurationFile)
  {
    this.configurationFile = new File(configurationFile);
  }
}

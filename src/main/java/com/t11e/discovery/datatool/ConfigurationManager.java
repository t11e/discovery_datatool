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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

@Component("ConfigurationManager")
public class ConfigurationManager
{
  private static final Logger logger =
    Logger.getLogger(ConfigurationManager.class.getName());

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

  public void loadConfiguration(
    final InputStream is,
    final boolean persist)
  {
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
        swapConfigFiles(config);
      }
      if (currentContext != null)
      {
        currentContext.stop();
        currentContext.close();
        currentContext = null;
      }
      currentContext = newContext;
    }
  }

  private void swapConfigFiles(final byte[] config)
  {
    try
    {
      if (contentsDiffer(config, configurationFile))
      {
        final File newConfig =
          File.createTempFile(configurationFile.getName(), ".tmp",
            configurationFile.getCanonicalFile().getParentFile());
        FileUtils.writeByteArrayToFile(newConfig, config);
        if (configurationFile.exists())
        {
          final File backupFile = new File(configurationFile + ".bak");
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
      }
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private boolean contentsDiffer(final byte[] config, final File configFile)
    throws FileNotFoundException, IOException
  {
    boolean changed = true;
    if (configFile.canRead())
    {
      final FileInputStream fis = new FileInputStream(configFile);
      try
      {
        if (IOUtils.contentEquals(new ByteArrayInputStream(config), fis))
        {
          changed = false;
        }
      }
      finally
      {
        IOUtils.closeQuietly(fis);
      }
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

    final SAXReader saxReader;
    if (false)
    {
      logger.log(Level.SEVERE, "Complex setup");
      saxReader = new SAXReader();
      try
      {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        final XMLReader xmlReader = factory.newSAXParser().getXMLReader();
        saxReader.setXMLReader(xmlReader);
        //saxReader.setFeature("http://xml.org/sax/features/namespaces", false);
        saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        //saxReader.setFeature("http://apache.org/xml/features/dom/include-ignorable-whitespace", false);
        saxReader.setValidation(true);
        saxReader.setEntityResolver(new PluggableSchemaResolver(null));
        //saxReader.setDocumentFactory( df() );
        saxReader.setStripWhitespaceText(true);
      }
      catch (final SAXException e1)
      {
        throw new RuntimeException(e1);
      }
      catch (final ParserConfigurationException e1)
      {
        throw new RuntimeException(e1);
      }
    }
    else
    {
      logger.log(Level.SEVERE, "Simple setup");
      saxReader = new SAXReader(false);
      saxReader.setIncludeExternalDTDDeclarations(false);
      saxReader.setIncludeInternalDTDDeclarations(true);
      saxReader.setEntityResolver(new EntityResolver()
      {
        public InputSource resolveEntity(final String publicId, final String systemId)
          throws SAXException, IOException
        {
          logger.log(Level.SEVERE, "resolveEntity " + publicId + " " + systemId);
          InputSource result = null;
          if ("YYY".equals(publicId))
          {
            final InputStream is = ConfigurationManager.class.getClassLoader().getResourceAsStream("discovery_datatool_config.dtd");
            logger.log(Level.SEVERE, "is: " + is);
            result = new InputSource();
          }
          return result;
        }
      });
      //saxReader.setEntityResolver(new PluggableSchemaResolver(null));
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
      for (final Node node : (List<Node>) document.selectNodes("/config/dataSources/dataSource"))
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
      for (final Node node : (List<Node>) document.selectNodes("/config/profiles/sqlProfile"))
      {
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlChangesetProfileService.class);
        builder.addPropertyValue("tableName", node.valueOf("@tableName"));
        builder.addPropertyValue("nameColumn", node.valueOf("@nameColumn"));
        builder.addPropertyValue("lastRunColumn", node.valueOf("@lastRunColumn"));
        applicationContext.registerBeanDefinition("profile-" + node.valueOf("@name"), builder.getBeanDefinition());
      }
    }
    {
      final List<ChangesetPublisher> publishers = new ArrayList<ChangesetPublisher>();
      for (final Node node : (List<Node>) document.selectNodes("/config/publishers/sqlPublisher"))
      {
        final List<SqlAction> actions = new ArrayList<SqlAction>();
        for (final Node action : (List<Node>) node.selectNodes("action"))
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlAction.class);
          builder.addPropertyValue("action", action.valueOf("@type"));
          builder.addPropertyValue("filter", action.valueOf("@filter"));
          builder.addPropertyValue("query", StringUtils.trimToEmpty(action.valueOf("query/text()")));
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

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
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.memory.InMemoryDaoImpl;
import org.springframework.security.core.userdetails.memory.UserMap;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.t11e.discovery.datatool.column.MergeColumns;

@Component("ConfigurationManager")
public class ConfigurationManager
{
  private static final Logger logger = Logger.getLogger(ConfigurationManager.class.getName());
  private static final List<GrantedAuthority> DEFAULT_ROLES =
      Arrays.asList((GrantedAuthority) new GrantedAuthorityImpl("ROLE_USER"));
  private File configurationFile;
  private File workingDirectory;
  private boolean exitOnInvalidConfigAtStartup;
  private ConfigurableApplicationContext currentContext;
  private BypassAuthenticationFilter bypassAuthenticationFilter;
  private InMemoryDaoImpl userDetailsService;

  @PostConstruct
  public void onPostConstruct()
  {
    if (configurationFile != null)
    {
      try
      {
        loadConfiguration(new FileInputStream(configurationFile), false);
        if (workingDirectory == null)
        {
          workingDirectory = new File(".").getCanonicalFile();
        }
      }
      catch (final RuntimeException e)
      {
        exitOnStartupIfConfigured(e);
        throw e;
      }
      catch (final FileNotFoundException e)
      {
        exitOnStartupIfConfigured(e);
        throw new RuntimeException(e);
      }
      catch (final IOException e)
      {
        exitOnStartupIfConfigured(e);
        throw new RuntimeException(e);
      }
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
    synchronized (this)
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
      applyAccessControl(new ByteArrayInputStream(config));
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
  private GenericApplicationContext createApplicationContext(final InputStream is)
  {
    final GenericApplicationContext applicationContext = new GenericApplicationContext();
    final Document document;
    final String ns;
    {
      final Document[] documentHolder = new Document[1];
      final String[] namespaceHolder = new String[1];
      parseConfiguration(is, documentHolder, namespaceHolder);
      document = documentHolder[0];
      ns = namespaceHolder[0];
    }

    if (document.selectSingleNode("/c:config".replace("c:", ns)) == null)
    {
      throw new RuntimeException("Missing root config element. Did you specify a namespace?");
    }

    for (final Node node : (List<Node>) document.selectNodes("/c:config/c:dataSources/c:dataSource".replace("c:", ns)))
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

    for (final Node node : (List<Node>) document.selectNodes("/c:config/c:dataSources/c:driver".replace("c:", ns)))
    {
      final String name = node.valueOf("@name");
      final Class<Driver> clazz = loadDataSource(node.valueOf("@class"), node.valueOf("@jar"));
      final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SimpleDriverDataSource.class);
      builder.addPropertyValue("driverClass", clazz);
      builder.addPropertyValue("url", node.valueOf("c:url".replace("c:", ns)));
      addPropertyIfExists(builder, "username", node, "c:username".replace("c:", ns));
      addPropertyIfExists(builder, "password", node, "c:password".replace("c:", ns));
      {
        final Map<String, String> properties = new HashMap<String, String>();
        for (final Node child : (List<Node>) node.selectNodes("c:properties/*".replace("c:", ns)))
        {
          properties.put(child.getName(), StringUtils.trimToEmpty(child.getText()));
        }
        if (!properties.isEmpty())
        {
          builder.addPropertyValue("connectionProperties", "");
        }
      }
      applicationContext.registerBeanDefinition("dataSource-" + name, builder.getBeanDefinition());
    }

    for (final Node node : (List<Node>) document.selectNodes("/c:config/c:profiles/c:sqlProfile".replace("c:", ns)))
    {
      final BeanDefinitionBuilder builder = BeanDefinitionBuilder
        .genericBeanDefinition(SqlChangesetProfileService.class);
      builder.addPropertyReference("dataSource", "dataSource-" + node.valueOf("@dataSource"));
      builder.addPropertyValue("createSql", node.valueOf("c:createSql".replace("c:", ns)));
      builder.addPropertyValue("retrieveStartColumn", node.valueOf("c:retrieveSql/@startColumn".replace("c:", ns)));
      builder.addPropertyValue("retrieveEndColumn", node.valueOf("c:retrieveSql/@endColumn".replace("c:", ns)));
      builder.addPropertyValue("retrieveSql", node.valueOf("c:retrieveSql".replace("c:", ns)));
      builder.addPropertyValue("updateSql", node.valueOf("c:updateSql".replace("c:", ns)));
      applicationContext.registerBeanDefinition("profile-" + node.valueOf("@name"), builder.getBeanDefinition());
    }
    {
      final List<ChangesetPublisher> publishers = new ArrayList<ChangesetPublisher>();
      for (final Node sqlPublisher : (List<Node>) document.selectNodes("/c:config/c:publishers/c:sqlPublisher"
        .replace("c:", ns)))
      {
        final List<SqlAction> filtered = new ArrayList<SqlAction>();
        final List<SqlAction> complete = new ArrayList<SqlAction>();
        final List<SqlAction> incremental = new ArrayList<SqlAction>();

        final PropertyCase propertyCase;
        {
          final String propCase = sqlPublisher.valueOf("@propertyCase");
          propertyCase = StringUtils.isBlank(propCase)
            ? PropertyCase.LEGACY
            : PropertyCase.valueOf(propCase.toUpperCase());
        }

        for (final Node action : (List<Node>) sqlPublisher.selectNodes("c:action".replace("c:", ns)))
        {
          defineAndInstantiateSqlAction(filtered, applicationContext, action, action.valueOf("@type"),
            action.valueOf("@filter"), propertyCase, ns);
        }
        for (final Node action : (List<Node>) sqlPublisher.selectNodes(
          "c:bulk/c:set-item | c:full/c:set-item"
          .replace("c:", ns)))
        {
          defineAndInstantiateSqlActionFromItemActionNode(complete, applicationContext, propertyCase, action, ns);
        }
        for (final Node action : (List<Node>) sqlPublisher.selectNodes(
          "c:snapshot/c:set-item | c:snapshot/c:remove-item | c:delta/c:add-to-item"
          .replace("c:", ns)))
        {
          defineAndInstantiateSqlActionFromItemActionNode(complete, applicationContext, propertyCase, action, ns);
        }
        for (final Node action : (List<Node>) sqlPublisher.selectNodes(
          "c:delta/c:set-item | c:delta/c:remove-item | c:delta/c:add-to-item"
          .replace("c:", ns)))
        {
          defineAndInstantiateSqlActionFromItemActionNode(incremental, applicationContext, propertyCase, action, ns);
        }
        BeanDefinition sqlChangesetExtractor;
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder
            .genericBeanDefinition(SqlChangesetExtractor.class);
          builder.addPropertyReference("dataSource", "dataSource-" + sqlPublisher.valueOf("@dataSource"));
          builder.addPropertyValue("filteredActions", filtered);
          builder.addPropertyValue("completeActions", complete);
          builder.addPropertyValue("incrementalActions", incremental);
          sqlChangesetExtractor = builder.getBeanDefinition();
        }
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ChangesetPublisher.class);
          final String name = sqlPublisher.valueOf("@name");
          builder.addPropertyValue("name", name);
          final String profile = sqlPublisher.valueOf("@profile");
          if (StringUtils.isNotBlank(profile))
          {
            builder.addPropertyReference("changesetProfileService", "profile-" + profile);
          }
          builder.addPropertyValue("changesetExtractor", sqlChangesetExtractor);
          final String beanName = "Publisher-" + name;
          applicationContext.registerBeanDefinition(beanName, builder.getBeanDefinition());
          publishers.add(applicationContext.getBean(beanName, ChangesetPublisher.class));
        }
      }
      {
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder
          .genericBeanDefinition(ChangesetPublisherManager.class);
        builder.addPropertyValue("publishers", publishers);
        applicationContext.registerBeanDefinition("ChangesetPublisherManager", builder.getBeanDefinition());
      }
    }
    applicationContext.refresh();
    return applicationContext;
  }

  private void defineAndInstantiateSqlActionFromItemActionNode(final List<SqlAction> target,
    final GenericApplicationContext applicationContext, final PropertyCase propertyCase, final Node action,
    final String ns)
  {
    final String filter = action.getParent().getName();
    final String type;
    if ("set-item".equals(action.getName()))
    {
      type = "create";
    }
    else if ("add-to-item".equals(action.getName()))
    {
      type = "add";
    }
    else
    {
      type = "delete";
    }
    defineAndInstantiateSqlAction(target, applicationContext, action, type, filter, propertyCase, ns);
  }

  private void defineAndInstantiateSqlAction(final List<SqlAction> actions,
    final GenericApplicationContext applicationContext, final Node setItem, final String action, final String filter,
    final PropertyCase propertyCase, final String ns)
  {
    final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlAction.class);
    builder.addPropertyValue("action", action);
    builder.addPropertyValue("filter", filter);
    builder.addPropertyValue("propertyCase", propertyCase);
    fillActionBeanDefinition(builder, setItem, propertyCase, ns);
    instantiateAction(actions, applicationContext, registerSqlAction(applicationContext, builder));
  }

  private void instantiateAction(final List<SqlAction> target, final GenericApplicationContext applicationContext,
    final String beanName)
  {
    target.add(applicationContext.getBean(beanName, SqlAction.class));
  }

  private String registerSqlAction(final GenericApplicationContext applicationContext,
    final BeanDefinitionBuilder builder)
  {
    final String beanName = "SqlAction-" + System.identityHashCode(builder);
    applicationContext.registerBeanDefinition(beanName, builder.getBeanDefinition());
    return beanName;
  }

  @SuppressWarnings("unchecked")
  private void fillActionBeanDefinition(final BeanDefinitionBuilder builder, final Node parentElementToQuery,
    final PropertyCase propertyCase, final String ns)
  {
    builder.addPropertyValue("idColumn", parentElementToQuery.valueOf("@idColumn"));
    addAttributeValueIfNotNull(builder, parentElementToQuery, "providerColumn");
    addAttributeValueIfNotNull(builder, parentElementToQuery, "kindColumn");
    {
      final String legacyScopedJsonColums = parentElementToQuery.valueOf("@jsonColumnNames");
      final String scopedJsonColumns = parentElementToQuery.valueOf("@scopedJsonColumns");
      if (StringUtils.isNotBlank(legacyScopedJsonColums) && StringUtils.isNotBlank(scopedJsonColumns))
      {
        throw new RuntimeException("You cannot specify both jsonColumnNames and scopedJsonColumnNames. Please just use scopedJsonColumnNames.");
      }
      final String columns = StringUtils.isNotBlank(scopedJsonColumns) ? scopedJsonColumns : legacyScopedJsonColums;
      builder.addPropertyValue("scopedJsonColumns", columns);
    }
    builder.addPropertyValue("unscopedJsonColumns", parentElementToQuery.valueOf("@unscopedJsonColumns"));
    builder
      .addPropertyValue("query",
        StringUtils.trimToEmpty(parentElementToQuery.valueOf("c:query/text()".replace("c:", ns))));

    {
      final List<MergeColumns> mergeColumns = new ArrayList<MergeColumns>();
      for (final Node mergeColumnDef : (List<Node>) parentElementToQuery.selectNodes("c:merge-columns"
        .replace("c:", ns)))
      {
        final String keyColumn = mergeColumnDef.valueOf("@keyColumn");
        final String valueColumn = mergeColumnDef.valueOf("@valueColumn");
        mergeColumns.add(new MergeColumns(keyColumn, valueColumn));
      }
      builder.addPropertyValue("mergeColumns", mergeColumns);
    }
    {
      final List<SubQuery> subqueries = new ArrayList<SubQuery>();
      for (final Node subquery : (List<Node>) parentElementToQuery.selectNodes("c:subquery".replace("c:", ns)))
      {
        final String sql = subquery.getText();
        final String property = propertyCase.convert(subquery.valueOf("@property"));
        final String propertyPrefix = propertyCase.convert(subquery.valueOf("@propertyPrefix"));
        if (StringUtils.isNotBlank(property) && StringUtils.isNotBlank(propertyPrefix))
        {
          throw new RuntimeException("Subqueries cannot specify both property and propertyPrefix: " + subquery.asXML());
        }
        String type = subquery.valueOf("@type");
        if (StringUtils.isBlank(type))
        {
          type = SubQuery.Type.ARRAY.name();
        }
        String delimiter = subquery.valueOf("@delimiter");
        if (StringUtils.isBlank(delimiter) && SubQuery.Type.DELIMITED.name().equalsIgnoreCase(type))
        {
          delimiter = ",";
        }
        final String discriminator = propertyCase.convert(subquery.valueOf("@discriminator"));
        if (StringUtils.isBlank(sql))
        {
          final Node publisher = subquery.selectSingleNode("../../..");
          logger.warning("Ignoring empty subquery element"
            + (publisher != null ? " in " + publisher.getName() + ": " + publisher.asXML() : ""));
        }
        else
        {
          subqueries.add(new SubQuery(SubQuery.Type.valueOf(type.toUpperCase()), sql, property, propertyPrefix,
            delimiter,
            discriminator));
        }
      }
      builder.addPropertyValue("subqueries", subqueries);
    }
  }

  private void addAttributeValueIfNotNull(final BeanDefinitionBuilder builder, final Node node, final String attribute)
  {

    final Node attrNode = node.selectSingleNode("@" + attribute);
    if (attrNode != null)
    {
      final String attrValue = attrNode.getText();
      builder.addPropertyValue(attribute, attrValue);
    }
  }

  @SuppressWarnings("unchecked")
  private void applyAccessControl(final InputStream is)
  {
    final Document document;
    final String ns;
    {
      final Document[] documentHolder = new Document[1];
      final String[] namespaceHolder = new String[1];
      parseConfiguration(is, documentHolder, namespaceHolder);
      document = documentHolder[0];
      ns = namespaceHolder[0];
    }
    bypassAuthenticationFilter.setBypass(false);
    userDetailsService.setUserMap(new UserMap());
    for (final Node node : (List<Node>) document.selectNodes("/c:config/c:accessControl/c:user".replace("c:", ns)))
    {
      userDetailsService.getUserMap().addUser(new User(node.valueOf("@name"),
        node.valueOf("@password"), true, true, true, true, DEFAULT_ROLES));
    }
    {
      final Node accessControl = document.selectSingleNode("/c:config/c:accessControl".replace("c:", ns));
      bypassAuthenticationFilter.setBypass(accessControl == null);
    }
  }

  @SuppressWarnings("unchecked")
  private void parseConfiguration(
    final InputStream is,
    final Document[] documentHolder,
    final String[] namespaceHolder)
  {
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
    final Map<String, String> namespacesByPrefix = CollectionsFactory.makeMap(
      "c1", "http://transparensee.com/schema/datatool-config-1",
      "c2", "http://transparensee.com/schema/datatool-config-2",
      "c3", "http://transparensee.com/schema/datatool-config-3",
      "c4", "http://transparensee.com/schema/datatool-config-4",
      "c5", "http://transparensee.com/schema/datatool-config-5",
      "c6", "http://transparensee.com/schema/datatool-config-6",
      "c7", "http://transparensee.com/schema/datatool-config-7");
    final Map<String, String> namespacesByUri = MapUtils.invertMap(namespacesByPrefix);
    {
      final DocumentFactory factory = new DocumentFactory();
      factory.setXPathNamespaceURIs(namespacesByPrefix);
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
    final String ns;
    {
      final String prefix = namespacesByUri.get(document.getRootElement().getNamespaceURI());
      ns = prefix == null ? "" : (prefix + ":");
    }
    namespaceHolder[0] = ns;
    documentHolder[0] = document;
  }

  private static void addPropertyIfExists(
    final BeanDefinitionBuilder builder,
    final String name,
    final Node node,
    final String xpath)
  {
    final String value = StringUtils.trimToEmpty(node.valueOf(xpath));
    if (StringUtils.isNotBlank(value))
    {
      builder.addPropertyValue(name, value);
    }
  }

  private <T> Class<T> loadDataSource(
    final String dataSourceClassName,
    final String jarPath)
  {
    URL jarUrl = null;
    try
    {
      if (StringUtils.isNotBlank(jarPath))
      {
        File jarFile = new File(jarPath);
        if (!jarFile.isAbsolute())
        {
          jarFile = new File(workingDirectory, jarPath);
        }
        jarUrl = jarFile.toURI().toURL();
      }
    }
    catch (final MalformedURLException e)
    {
      throw new RuntimeException(e);
    }
    return loadDataSource(dataSourceClassName, jarUrl);
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> loadDataSource(
    final String dataSourceClassName,
    final URL jarUrl)
  {
    Class<T> driverClass;
    try
    {
      if (jarUrl == null)
      {
        driverClass = (Class<T>) Class.forName(dataSourceClassName);
      }
      else
      {
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl});
        driverClass = (Class<T>) classLoader.loadClass(dataSourceClassName);
      }
    }
    catch (final ClassCastException e)
    {
      throw new RuntimeException(dataSourceClassName + " is not a DataSource or Driver, from " + jarUrl, e);
    }
    catch (final ClassNotFoundException e)
    {
      throw new RuntimeException("Could not find the DataSource or Driver: " + dataSourceClassName + " from " + jarUrl,
        e);
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

  public void setWorkingDirectory(final String workingDirectory)
  {
    try
    {
      this.workingDirectory = new File(workingDirectory).getCanonicalFile();
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Resource(name = "BypassAuthenticationFilter")
  public void setBypassAuthenticationFilter(
    final BypassAuthenticationFilter bypassAuthenticationFilter)
  {
    this.bypassAuthenticationFilter = bypassAuthenticationFilter;
  }

  @Autowired
  public void setInMemoryDaoImpl(final InMemoryDaoImpl inMemoryDaoImpl)
  {
    userDetailsService = inMemoryDaoImpl;
  }
}

package com.t11e.discovery.datatool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.sql.DataSource;
import javax.xml.stream.XMLStreamException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = {"applicationContext-test.xml"})
public abstract class EndToEndTestBase
{

  @Autowired
  protected ChangesetController changesetController;
  @Autowired
  protected ConfigurationManager configurationManager;
  protected NamedParameterJdbcTemplate template;

  @Before
  public final void setUp()
  {
    configurationManager.loadConfiguration(getConfigurationXml(), false);
    template = new NamedParameterJdbcTemplate(configurationManager.getBean(DataSource.class));
    executeSqlScripts(getSetupScripts());
  }

  @After
  public final void tearDown()
  {
    executeSqlScripts(getCleanupScripts());
  }

  protected abstract InputStream getConfigurationXml();

  protected String[] getCleanupScripts()
  {
    return new String[]{};
  }

  protected String[] getSetupScripts()
  {
    return new String[]{};
  }


  protected void executeSqlScripts(final String... scriptNames)
  {
    final DataSource dataSource = configurationManager.getBean(DataSource.class);
    final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    final Resource[] resources = new Resource[scriptNames.length];
    for (int i = 0; i < scriptNames.length; i++)
    {
      resources[i] = new ClassPathResource(scriptNames[i], getClass());
    }
    populator.setScripts(resources);
    try
    {
      final Connection connection = dataSource.getConnection();
      populator.populate(connection);
      connection.close();
    }
    catch (final SQLException e)
    {
      throw new RuntimeException(e);
    }
  }

  protected List<String> nodesAsStrings(final Document doc, final String xpath)
  {
    final List<String> result = new ArrayList<String>();
    for (final Object node : doc.selectNodes(xpath))
    {
      result.add(((Node) node).getText());
    }
    return result;
  }

  protected Document parseXmlResponse(final MockHttpServletResponse response)
    throws DocumentException
  {
    final SAXReader saxReader = new SAXReader();
    return saxReader.read(new ByteArrayInputStream(response.getContentAsByteArray()));
  }

  protected Document assertChangeset(final String publisher, final String profile, final String expectedType,
    final Collection<String> expectedSetItemIds, final Collection<String> expectedRemoveItemIds,
    final boolean forceSnapshot)
  {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    try
    {
      changesetController.publish(request, response,
        publisher, null, null, profile, false, forceSnapshot);
    }
    catch (final XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
    Assert.assertEquals(200, response.getStatus());
    Assert.assertEquals("text/xml; charset=utf-8", response.getContentType());
    Assert.assertEquals(expectedType, response.getHeader("X-t11e-type"));
    Document doc;
    try
    {
      doc = parseXmlResponse(response);
    }
    catch (final DocumentException e)
    {
      throw new RuntimeException(e);
    }
    Assert.assertEquals(expectedSetItemIds.size(), doc.selectNodes("/changeset/set-item").size());
    Assert.assertEquals(
      new HashSet<String>(expectedSetItemIds),
      new HashSet<String>(nodesAsStrings(doc, "/changeset/set-item/@id")));
    Assert.assertEquals(expectedRemoveItemIds.size(), doc.selectNodes("/changeset/remove-item").size());
    Assert.assertEquals(
      new HashSet<String>(expectedRemoveItemIds),
      new HashSet<String>(nodesAsStrings(doc, "/changeset/remove-item/@id")));
    return doc;
  }

}

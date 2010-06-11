package com.t11e.discovery.datatool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.stream.XMLStreamException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
  locations={"applicationContext-test.xml"})
public class IntegrationTest
{
  @Autowired
  private ChangesetController changesetController;
  @Autowired
  private ConfigurationManager configurationManager;
  private NamedParameterJdbcTemplate template;

  @Before
  public void setup()
  {
    template = new NamedParameterJdbcTemplate(configurationManager.getBean(DataSource.class));
    executeSqlScripts("IntegrationTestCreate.sql");
  }

  @After
  public void teardown()
  {
    executeSqlScripts("IntegrationTestDrop.sql");
  }

  @Test
  public void testNoProfileNoRange()
    throws XMLStreamException, IOException, DocumentException
  {
    assertChangeset("test-all", "", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testProfileWithModifiedRows()
    throws XMLStreamException, IOException, DocumentException
  {
    // Snapshot with no lastRun date
    assertChangeset("test", "test", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"));
    assertChangeset("test", "test", "delta",
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST);
    // Touch two rows and get another delta
    {
      final Date origLastRun = template.queryForObject(
        "select lastRun from IntegrationProfile where name = 'test'", (Map) null, Date.class);
      final Date lastUpdated = new Date(origLastRun.getTime() - (60 * 1000));
      template.update(
        "update IntegrationProfile " +
        "set lastRun = :lastRun " +
        "where name = 'test'",
        CollectionsFactory.makeMap(
          "lastRun", lastUpdated));
      template.update(
        "update IntegrationContent " +
        "set lastUpdated = :lastUpdated " +
        "where id in (:ids)",
        CollectionsFactory.makeMap(
          "lastUpdated", lastUpdated,
          "ids", CollectionsFactory.makeList(1, 3)
        ));
    }
    assertChangeset("test", "test", "delta",
      CollectionsFactory.makeList("1", "3"),
      Collections.EMPTY_LIST);
    assertChangeset("test", "test", "delta",
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testProfileWithDeletedRows()
    throws XMLStreamException, IOException, DocumentException
  {
    // Snapshot with no lastRun date
    assertChangeset("test", "test", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"));
    assertChangeset("test", "test", "delta",
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST);
    // Delete two rows and get another delta
    {
      final Date origLastRun = template.queryForObject(
        "select lastRun from IntegrationProfile where name = 'test'", (Map) null, Date.class);
      final Date lastUpdated = new Date(origLastRun.getTime() - (60 * 1000));
      template.update(
        "update IntegrationProfile " +
        "set lastRun = :lastRun " +
        "where name = 'test'",
        CollectionsFactory.makeMap(
          "lastRun", lastUpdated));
      template.update(
        "insert into IntegrationDeleted " +
        "(id, lastUpdated) " +
        "values " +
        "(1, :dateDeleted), " +
        "(3, :dateDeleted)",
        CollectionsFactory.makeMap(
          "dateDeleted", lastUpdated));
    }
    assertChangeset("test", "test", "delta",
      Collections.EMPTY_LIST,
      CollectionsFactory.makeList("1", "3"));
    assertChangeset("test", "test", "delta",
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST);
  }

  @Test
  public void testXmlEscaping() throws Exception
  {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    changesetController.publish(request, response,
      "test-xml-escaping", null, null, "", false);
    Assert.assertEquals(200, response.getStatus());
    Assert.assertEquals("text/xml; charset=utf-8", response.getContentType());
    Assert.assertEquals("snapshot", response.getHeader("X-t11e-type"));
    Assert.assertFalse("Should contain escaped XML",
      response.getContentAsString().contains("Hello & < > goodbye"));
    Assert.assertTrue("Should contain escaped XML",
      response.getContentAsString().contains("Hello &amp; &lt; > \" ' goodbye"));
    final Document doc = parseXmlResponse(response);
    Assert.assertEquals("Hello & < > \" ' goodbye", doc.valueOf(
      "/changeset/set-item[@id='1']/properties/struct/entry[@name='text_content']/string"));
  }

  @SuppressWarnings("unchecked")
  private void assertChangeset(
    final String publisher,
    final String profile,
    final String expectedType,
    final Collection<String> expectedSetItemIds, final Collection<String> expectedRemoveItemIds)
    throws XMLStreamException, IOException, DocumentException
  {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    changesetController.publish(request, response,
      publisher, null, null, profile, false);
    Assert.assertEquals(200, response.getStatus());
    Assert.assertEquals("text/xml; charset=utf-8", response.getContentType());
    Assert.assertEquals(expectedType, response.getHeader("X-t11e-type"));
    final Document doc = parseXmlResponse(response);
    Assert.assertEquals(expectedSetItemIds.size(), doc.selectNodes("/changeset/set-item").size());
    Assert.assertEquals(
      new HashSet(expectedSetItemIds),
      new HashSet(nodesAsStrings(doc, "/changeset/set-item/@id")));
    Assert.assertEquals(expectedRemoveItemIds.size(), doc.selectNodes("/changeset/remove-item").size());
    Assert.assertEquals(
      new HashSet(expectedRemoveItemIds),
      new HashSet(nodesAsStrings(doc, "/changeset/remove-item/@id")));
  }

  private List<String> nodesAsStrings(final Document doc, final String xpath)
  {
    final List<String> result = new ArrayList<String>();
    for (final Object node : doc.selectNodes(xpath))
    {
      result.add(((Node) node).getText());
    }
    return result;
  }

  private Document parseXmlResponse(final MockHttpServletResponse response)
    throws DocumentException
  {
    final SAXReader saxReader = new SAXReader();
    return saxReader.read(new ByteArrayInputStream(response.getContentAsByteArray()));
  }

  private void executeSqlScripts(final String... scriptNames)
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
}

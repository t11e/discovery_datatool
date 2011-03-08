package com.t11e.discovery.datatool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.dom4j.Document;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class IntegrationTestBase
  extends EndToEndTestBase
{
  @Override
  protected String[] getSetupScripts()
  {
    return new String[]{"IntegrationTestCreate.sql"};
  }

  @Override
  protected String[] getCleanupScripts()
  {
    return new String[]{"IntegrationTestDrop.sql"};
  }


  @Test
  public void testNoProfileNoRange()
  {
    assertChangeset("test-all", "", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), false);
  }

  @Test
  public void testNoProfileNoRangeStoredProc()
  {
    assertChangeset("test-stored-proc", "", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), false);
  }

  @Test
  public void testStoredProcProfileWithModifiedRows()
  {
    testProfileWithModifiedRows("test-stored-proc");
  }

  @Test
  public void testProfileWithModifiedRows()
  {
    testProfileWithModifiedRows("test");
  }

  private void testProfileWithModifiedRows(final String publisher)
  {
    // Snapshot with no lastRun date
    assertChangeset(publisher, "test", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), false);
    assertChangeset(publisher, "test", "delta",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(), false);
    // Touch two rows and get another delta
    {
      final Date origLastRun = template.queryForObject(
        "select lastRun from IntegrationProfile where name = 'test'", (Map<String, Object>) null, Date.class);
      final Date lastUpdated = new Date(origLastRun.getTime() - (60 * 1000));
      template.update(
        "update IntegrationProfile " +
          "set lastRun = :lastRun " +
          "where name = 'test'",
        CollectionsFactory.<String, String> makeMap(
          "lastRun", lastUpdated));
      template.update(
        "update IntegrationContent " +
          "set lastUpdated = :lastUpdated " +
          "where id in (:ids)",
        CollectionsFactory.<String, String> makeMap(
          "lastUpdated", lastUpdated,
          "ids", CollectionsFactory.makeList(1, 3)
          ));
    }
    assertChangeset(publisher, "test", "delta",
      CollectionsFactory.makeList("1", "3"),
      Collections.<String> emptyList(), false);
    assertChangeset(publisher, "test", "delta",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(), false);

    assertChangeset(publisher, "test", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), true);
    assertChangeset(publisher, "test", "delta",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testProfileWithDeletedRows()
  {
    // Snapshot with no lastRun date
    assertChangeset("test", "test", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), false);
    assertChangeset("test", "test", "delta",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(), false);
    // Delete two rows and get another delta
    {
      final Date origLastRun = template.queryForObject(
        "select lastRun from IntegrationProfile where name = 'test'", (Map<String, Object>) null, Date.class);
      final Date lastUpdated = new Date(origLastRun.getTime() - (60 * 1000));
      template.update(
        "update IntegrationProfile " +
          "set lastRun = :lastRun " +
          "where name = 'test'",
        CollectionsFactory.<String, String> makeMap(
          "lastRun", lastUpdated));
      template.update(
        "insert into IntegrationDeleted " +
          "(id, lastUpdated) " +
          "values " +
          "(1, :dateDeleted), " +
          "(3, :dateDeleted)",
        CollectionsFactory.<String, String> makeMap(
          "dateDeleted", lastUpdated));
    }
    assertChangeset("test", "test", "delta",
      Collections.<String> emptyList(),
      CollectionsFactory.makeList("1", "3"), false);
    assertChangeset("test", "test", "delta",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testXmlEscaping()
    throws Exception
  {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    changesetController.publish(request, response,
      "test-xml-escaping", null, null, "", false, false);
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

  public static void integrationContentSP(final Timestamp start, final Timestamp end, final ResultSet[] result)
    throws SQLException
  {
    final Connection conn = DriverManager.getConnection("jdbc:default:connection");
    final PreparedStatement ps;
    if (start == null)
    {
      ps = conn.prepareStatement("select * from IntegrationContent");
    }
    else
    {
      ps = conn.prepareStatement("select * from IntegrationContent where lastUpdated >= ? and lastUpdated < ?");
      ps.setTimestamp(1, start);
      ps.setTimestamp(2, end);
    }
    result[0] = ps.executeQuery();
    conn.close();
  }
}

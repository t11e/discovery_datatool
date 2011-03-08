package com.t11e.discovery.datatool;

import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class BulkAndFullChangesetTest
  extends EndToEndTestBase
{

  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("BulkAndFullChangesetTest.xml");
  }

  @Override
  protected String[] getSetupScripts()
  {
    return new String[]{"BulkAndFullChangesetTestCreate.sql"};
  }

  @Override
  protected String[] getCleanupScripts()
  {
    return new String[]{"BulkAndFullChangesetTestDrop.sql"};
  }

  @Test
  public void testBulk()
  {
    assertChangeset("test-bulk", "", "bulk",
      CollectionsFactory.makeList("1", "2", "3"),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testBulkAndDelta()
  {
    assertChangeset("test-bulk-and-delta", "", "bulk",
      CollectionsFactory.makeList("1", "2", "3"),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testFull()
  {
    assertChangeset("test-full", "", "full",
      CollectionsFactory.makeList("1", "2", "3"),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testFullAndDelta()
  {
    assertChangeset("test-full-and-delta", "", "full",
      CollectionsFactory.makeList("1", "2", "3"),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testSnapshot()
  {
    assertChangeset("test-snapshot", "", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), false);
  }

  @Test
  public void testSnapshotAndDelta()
  {
    assertChangeset("test-snapshot-and-delta", "", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), false);
  }

  @Test
  public void testProfileWithModifedRowsSnapshot()
  {
    testProfileWithModifiedRows("test-snapshot-and-delta", "snapshot", CollectionsFactory.makeList("4", "5"));
  }

  @Test
  public void testProfileWithModifedRowsFull()
  {
    testProfileWithModifiedRows("test-full-and-delta", "full", Collections.<String> emptyList());
  }

  @Test
  public void testProfileWithModifedRowsBulk()
  {
    testProfileWithModifiedRows("test-bulk-and-delta", "bulk", Collections.<String> emptyList());
  }

  private void testProfileWithModifiedRows(final String publisher, final String expectedCompleteType, final List<String> expectedDeletedIds)
  {
    // Snapshot with no lastRun date
    assertChangeset(publisher, "test", expectedCompleteType,
      CollectionsFactory.makeList("1", "2", "3"),
      expectedDeletedIds, false);

    assertChangeset(publisher, "test", "delta",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(), false);
    // Touch two rows and get another delta
    {
      final Date origLastRun = template.queryForObject(
        "select lastRun from BulkAndFullChangesetTestProfile where name = 'test'", (Map<String, Object>) null, Date.class);
      final Date lastUpdated = new Date(origLastRun.getTime() - (60 * 1000));
      template.update(
        "update BulkAndFullChangesetTestProfile " +
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

    assertChangeset(publisher, "test", expectedCompleteType,
      CollectionsFactory.makeList("1", "2", "3"),
      expectedDeletedIds, true);
    assertChangeset(publisher, "test", "delta",
      Collections.<String> emptyList(),
      Collections.<String> emptyList(), false);
  }
}

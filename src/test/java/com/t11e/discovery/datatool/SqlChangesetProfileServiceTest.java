package com.t11e.discovery.datatool;

import java.util.Date;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = {"applicationContext-test.xml"})
public class SqlChangesetProfileServiceTest
{
  @Autowired
  private DataSource dataSource;
  private SqlChangesetProfileService profileService;

  @Before
  public void setup()
  {
    profileService = new SqlChangesetProfileService();
    profileService.setDataSource(dataSource);
    profileService.setRetrieveSql(
      "select last_run as \"startTime\", CURRENT_TIME as \"endTime\" " +
        "from profile_table " +
        "where name = :name");
    profileService.setRetrieveStartColumn("StartTimE");
    profileService.setRetrieveEndColumn("endtime");
    profileService.setUpdateSql(
      "update profile_table " +
        "set last_run = :lastRun " +
        "where name = :name");
  }

  @Test
  public void testNoProfile()
  {
    assertNoProfile("");
    assertNoProfile("invalid");
  }

  @Test
  public void testValidProfile()
  {
    assertValidProfile("test", null, false);
    assertValidProfile("other", null, false);
  }

  @Test
  public void testAutoCreateProfile()
  {
    profileService.setCreateSql("insert into profile_table (name) values (:name)");
    assertValidProfile("autocreate1", null, false);

    final Date run1 = new Date();
    final Date run2 = new Date(run1.getTime() + (1000 * 60));
    profileService.saveChangesetProfileLastRun("autocreate1", run2);
    assertValidProfile("autocreate1", run2, false);
    assertValidProfile("other", null, false);

    profileService.saveChangesetProfileLastRun("autocreate1", run2);
    assertValidProfile("autocreate1", run2, false);
    assertValidProfile("other", null, false);
  }

  @Test
  public void testDryRunDoesNotAutoCreateProfile()
  {
    profileService.setCreateSql("insert into profile_table (name) values (:name)");
    assertNoProfile("autocreate2");
  }

  @Test
  public void testProfileUpdate()
  {
    final Date run1 = new Date();
    assertValidProfile("test", null, false);
    assertValidProfile("other", null, false);

    profileService.saveChangesetProfileLastRun("test", run1);
    assertValidProfile("test", run1, false);
    assertValidProfile("other", null, false);

    final Date run2 = new Date(run1.getTime() + (1000 * 60));
    profileService.saveChangesetProfileLastRun("test", run2);
    assertValidProfile("test", run2, false);
    assertValidProfile("other", null, false);

    profileService.saveChangesetProfileLastRun("test", run2);
    assertValidProfile("test", run2, false);
    assertValidProfile("other", null, false);
  }

  @Test
  public void testInvalidProfileUpdate()
  {
    try
    {
      profileService.saveChangesetProfileLastRun("invalid", new Date());
      Assert.fail("Expected RuntimeException");
    }
    catch (final RuntimeException e)
    {
      // Good
    }
  }

  private void assertValidProfile(
    final String profile,
    final Date start,
    final boolean dryRun)
  {
    final Date[] range = profileService.getChangesetProfileDateRange(profile, dryRun);
    Assert.assertNotNull(range);
    Assert.assertEquals(2, range.length);
    Assert.assertEquals(start, range[0]);
    Assert.assertNotNull(range[1]);
  }

  private void assertNoProfile(final String profile)
  {
    try
    {
      profileService.getChangesetProfileDateRange(profile, true);
      Assert.fail("Expected NoSuchProfileException");
    }
    catch (final NoSuchProfileException e)
    {
      // Good
    }
  }
}

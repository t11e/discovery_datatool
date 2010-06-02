package com.t11e.discovery.datatool;

import java.util.Date;

import javax.sql.DataSource;
import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
  locations={"applicationContext-test.xml"})
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
      "select last_run as startTime, CURRENT_TIME as endTime " +
      "from profile_table " +
      "where name = :name");
    profileService.setRetrieveStartColumn("startTime");
    profileService.setRetrieveEndColumn("endTime");
    profileService.setUpdateSql(
      "update profile_table " +
      "set last_run = :lastRun " +
      "where name = :name");
  }

  @Test
  public void testNoProfile()
    throws XMLStreamException
  {
    assertNoProfile("");
    assertNoProfile("invalid");
  }

  @Test
  public void testValidProfile()
  {
    assertValidProfile("test", null);
    assertValidProfile("other", null);
  }

  @Test
  public void testProfileUpdate()
  {
    final Date run1 = new Date();
    assertValidProfile("test", null);
    assertValidProfile("other", null);

    profileService.saveChangesetProfileLastRun("test", run1);
    assertValidProfile("test", run1);
    assertValidProfile("other", null);

    final Date run2 = new Date(run1.getTime() + (1000 * 60));
    profileService.saveChangesetProfileLastRun("test", run2);
    assertValidProfile("test", run2);
    assertValidProfile("other", null);

    profileService.saveChangesetProfileLastRun("test", run2);
    assertValidProfile("test", run2);
    assertValidProfile("other", null);
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

  private void assertValidProfile(final String profile, final Date start)
  {
    final Date[] range = profileService.getChangesetProfileDateRange(profile);
    Assert.assertNotNull(range);
    Assert.assertEquals(2, range.length);
    Assert.assertEquals(start, range[0]);
    Assert.assertNotNull(range[1]);
  }

  private void assertNoProfile(final String profile)
  {
    try
    {
      profileService.getChangesetProfileDateRange(profile);
      Assert.fail("Expected NoSuchProfileException");
    }
    catch (final NoSuchProfileException e)
    {
      // Good
    }
  }
}

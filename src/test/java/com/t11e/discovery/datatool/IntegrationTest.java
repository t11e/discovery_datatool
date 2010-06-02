package com.t11e.discovery.datatool;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
  locations={"applicationContext-test.xml"})
public class IntegrationTest
{
  private ConfigurationManager configurationManager;
  private ChangesetPublisherManager publisherManager;

  @Before
  public void setup()
    throws URISyntaxException, SQLException
  {
    configurationManager = new ConfigurationManager();
    configurationManager.setConfigurationFile(
      new File(getClass().getResource("IntegrationTest.xml").toURI()).getPath());
    configurationManager.onPostConstruct();
    publisherManager = configurationManager.getBean(ChangesetPublisherManager.class);
    Assert.assertNotNull(publisherManager);
    {
      final DataSource dataSource = configurationManager.getBean(DataSource.class);
      final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
      populator.setScripts(new Resource[] {
          new ClassPathResource("IntegrationTest.sql", getClass())
      });
      final Connection connection = dataSource.getConnection();
      populator.populate(connection);
      connection.close();
    }
  }

  @Test
  public void test()
  {
    final ChangesetPublisher publisher = publisherManager.getChangesetPublisher("test");
    Assert.assertNotNull(publisher);
    final ChangesetProfileService profileService = publisher.getChangesetProfileService();
    final Date[] range = profileService.getChangesetProfileDateRange("test");
    Assert.assertNotNull(range);
  }
}

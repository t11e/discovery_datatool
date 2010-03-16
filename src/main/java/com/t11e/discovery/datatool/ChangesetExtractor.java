package com.t11e.discovery.datatool;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ChangesetExtractor
{
  @Autowired
  private DataSource dataSource;
  private NamedParameterJdbcTemplate template;
  private List<SqlQueryTemplate> selectTemplates;

  @PostConstruct
  public void initialize()
  {
    template = new NamedParameterJdbcTemplate(dataSource);
  }

  public void getChangesetForRange(
    final OutputStream os,
    final Date start,
    final Date end)
  {
    try
    {
      os.write("<changeset/>".getBytes());
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Required
  public void setSelectTemplates(final List<SqlQueryTemplate> selectTemplates)
  {
    this.selectTemplates = selectTemplates;
  }
}

package com.t11e.discovery.datatool;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ChangesetExtractor
{
  @Autowired
  private DataSource dataSource;
  private NamedParameterJdbcTemplate jdbcTemplate;
  private List<SqlTemplate> sqlTemplates;

  @PostConstruct
  public void initialize()
  {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  public void getChangeset(
    final OutputStream os,
    final String changesetType,
    final Date start,
    final Date end)
  {
    try
    {
      final XMLStreamWriter xml = StaxUtil.newOutputFactory().createXMLStreamWriter(os);
      xml.writeStartDocument();
      xml.writeStartElement("changeset");
      final ChangesetWriter writer = new XmlChangesetWriter(xml);
      for (final SqlTemplate sqlTemplate : sqlTemplates)
      {
        if (sqlTemplate.getFilter().contains(changesetType))
        {
          process(writer, sqlTemplate, changesetType, start, end);
        }
      }
      xml.writeEndElement();
      xml.writeEndDocument();
      xml.flush();
    }
    catch (final XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void process(
    final ChangesetWriter writer,
    final SqlTemplate sqlTemplate,
    final String kind,
    final Date start,
    final Date end)
  {
    final MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("start", start);
    params.addValue("end", end);
    params.addValue("kind", kind);
    final RowCallbackHandler callbackHandler;
    if ("create".equals(sqlTemplate.getAction()))
    {
      // TODO
      callbackHandler = null;
    }
    else if ("delete".equals(sqlTemplate.getAction()))
    {
      callbackHandler =
        new DeleteActionRowCallbackHandler(writer, sqlTemplate.getIdColumn());
    }
    else
    {
      throw new RuntimeException("Unknown action: " + sqlTemplate.getAction());
    }
    jdbcTemplate.query(sqlTemplate.getQuery(), params, callbackHandler);
  }

  @Required
  public void setSqlTemplates(final List<SqlTemplate> selectTemplates)
  {
    this.sqlTemplates = selectTemplates;
  }
}

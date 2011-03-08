package com.t11e.discovery.datatool;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ChangesetController
{
  @Autowired
  private ChangesetService changesetService;

  private static Format HTTP_DATE_FORMAT = FastDateFormat.getInstance(
    "EEE, dd MMM yyyy HH:mm:ss zzz",
    TimeZone.getTimeZone("GMT"),
    Locale.US);

  @InitBinder()
  public void initBinding(final WebDataBinder binder)
  {
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dateFormat.setLenient(false);
    binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true, 19));
  }

  @RequestMapping("/ws/publisher/{publisherName}")
  public void publish(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("publisherName") final String publisherName,
    @RequestParam(value = "startDate", defaultValue = "", required = false) final Date startParam,
    @RequestParam(value = "endDate", defaultValue = "", required = false) final Date endParam,
    @RequestParam(value = "profile", defaultValue = "", required = false) final String profile,
    @RequestParam(value = "dryRun", defaultValue = "false", required = false) final boolean dryRun,
    @RequestParam(value = "forceSnapshot", defaultValue = "false", required = false) final boolean forceSnapshot)
    throws XMLStreamException, IOException
  {
    final ChangesetPublisher changesetPublisher =
        changesetService.getChangesetPublisher(publisherName);
    if (changesetPublisher == null)
    {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    final ChangesetProfileService changesetProfileService =
        changesetPublisher.getChangesetProfileService();
    final Date start;
    final Date end;
    if (StringUtils.isBlank(profile) || changesetProfileService == null)
    {
      start = startParam;
      end = endParam;
    }
    else
    {
      try
      {
        final Date[] range = changesetProfileService.getChangesetProfileDateRange(profile, dryRun);
        start = forceSnapshot ? null : range[0];
        end = range[1];
      }
      catch (final NoSuchProfileException e)
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        return;
      }
    }
    publishImpl(request, response, changesetPublisher.getChangesetExtractor(), start, end);
    if (StringUtils.isNotBlank(profile) && !dryRun && changesetProfileService != null)
    {
      changesetProfileService.saveChangesetProfileLastRun(profile, end);
    }
  }

  public void publishImpl(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final ChangesetExtractor changesetExtractor,
    final Date start,
    final Date end)
    throws XMLStreamException, IOException
  {
    final String changesetType = changesetExtractor.determineType(start);
    response.setContentType("text/xml; charset=utf-8");
    response.setHeader("Date",
      HTTP_DATE_FORMAT.format(end != null ? end : new Date()));
    response.setHeader("X-t11e-type", changesetType);
    final OutputStream os = HttpUtil.getCompressedResponseStream(request, response);
    {
      final XMLStreamWriter xml =
          StaxUtil.newOutputFactory().createXMLStreamWriter(os);
      xml.writeStartDocument();
      xml.writeCharacters("\n");
      xml.writeStartElement("changeset");
      xml.writeCharacters("\n");
      changesetExtractor.writeChangeset(new XmlChangesetWriter(xml),
        changesetType, start, end);
      xml.writeEndElement();
      xml.writeCharacters("\n");
      xml.writeEndDocument();
      xml.flush();
    }
    if (os instanceof GZIPOutputStream)
    {
      final GZIPOutputStream gos = (GZIPOutputStream) os;
      gos.finish();
    }
  }

  public void setChangesetService(final ChangesetService changesetService)
  {
    this.changesetService = changesetService;
  }
}

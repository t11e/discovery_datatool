package com.t11e.discovery.datatool;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
  private static final Logger logger = Logger.getLogger(ChangesetController.class.getName());

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
    throws Exception
  {
    logger.info("Processing changeset request from " + request.getRemoteAddr() + " for publisher '" + publisherName
      + "' with parameters:" + " startDate=" + startParam + " endDate=" + endParam + " profile=" + profile
      + " dryRun=" + dryRun + " forceSnapshot=" + forceSnapshot);
    final ChangesetPublisher changesetPublisher =
        changesetService.getChangesetPublisher(publisherName);
    if (changesetPublisher == null)
    {
      logger.warning("Changeset publisher does not exist: " + publisherName);
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    final ChangesetProfileService changesetProfileService =
        changesetPublisher.getChangesetProfileService();
    final Date start;
    final Date end;
    if (StringUtils.isBlank(profile) || changesetProfileService == null)
    {
      logger.fine("Running without a changeset profile: start=" + startParam + " end=" + endParam);
      start = startParam;
      end = endParam;
    }
    else
    {
      logger.fine("Will use changeset profile: " + profile);
      try
      {
        final Date[] range = changesetProfileService.getChangesetProfileDateRange(profile, dryRun);
        logger.fine("Loaded changeset profile: start=" + range[0] + " end=" + range[1]);
        start = forceSnapshot ? null : range[0];
        end = range[1];
        if (forceSnapshot && range[0] != null)
        {
          logger.fine("Ignoring lastRun from changeset profile because of forceSnapshot query parameter");
        }
      }
      catch (final NoSuchProfileException e)
      {
        logger.warning("Missing changeset profile: " + profile);
        response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        return;
      }
    }
    logger.fine("Generating changeset: start=" + start + " end=" + end);
    final String summary = publishImpl(request, response, changesetPublisher.getChangesetExtractor(), start, end);
    logger.fine("Changeset successfully generated");
    if (StringUtils.isBlank(profile) || changesetProfileService == null)
    {
      // No profile in play
    }
    else if (dryRun)
    {
      logger.warning("Will not update changeset profile because of dryRun query parameter");
    }
    else
    {
      logger.fine("Saving changeset profile '" + profile + "' with lastRun " + end);
      changesetProfileService.saveChangesetProfileLastRun(profile, end);
    }
    logger.info("Successfully finished processing changeset request: " + summary + " start=" + start + " end=" + end);
  }

  public String publishImpl(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final ChangesetExtractor changesetExtractor,
    final Date start,
    final Date end)
    throws Exception
  {
    final String changesetType = changesetExtractor.determineType(start);
    response.setContentType("text/xml; charset=utf-8");
    response.setHeader("Date",
      HTTP_DATE_FORMAT.format(end != null ? end : new Date()));
    response.setHeader("X-t11e-type", changesetType);
    final OutputStream os = HttpUtil.getCompressedResponseStream(request, response);
    final PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "utf-8"));
    final XMLStreamWriter xml = StaxUtil.newOutputFactory().createXMLStreamWriter(writer);
    try
    {
      xml.writeStartDocument();
      xml.writeCharacters("\n");
      xml.writeStartElement("changeset");
      xml.writeCharacters("\n");
      final XmlChangesetWriter changesetWriter = new XmlChangesetWriter(xml);
      try
      {
        changesetExtractor.writeChangeset(changesetWriter, changesetType, start, end);
      }
      catch (final RuntimeException e)
      {
        if (!response.isCommitted())
        {
          response.reset();
          throw e;
        }
        xml.flush();
        reportAndRethrowException(writer, e);
      }
      catch (final Exception e)
      {
        if (!response.isCommitted())
        {
          response.reset();
          throw new RuntimeException(e);
        }
        xml.flush();
        reportAndRethrowException(writer, e);
      }
      xml.writeEndElement();
      xml.writeCharacters("\n");
      xml.writeEndDocument();
      xml.flush();
      return "type=" + changesetType + " summary={" + changesetWriter.summarizeActions() + "}";
    }
    finally
    {
      if (os instanceof GZIPOutputStream)
      {
        final GZIPOutputStream gos = (GZIPOutputStream) os;
        gos.finish();
      }
    }
  }

  private static void reportAndRethrowException(final PrintWriter writer, final Exception e)
    throws Exception
  {
    writer.println();
    writer.println();
    writer.println();
    writer.println(
      "*** An exception occured. Since the response has already been committed, we're causing the changeset " +
        "to be invalid, and including the exception details here. " + e.getLocalizedMessage());
    writer.print("*** Exception was: ");
    e.printStackTrace(writer);
    writer.flush();
    throw e;
  }

  public void setChangesetService(final ChangesetService changesetService)
  {
    this.changesetService = changesetService;
  }
}

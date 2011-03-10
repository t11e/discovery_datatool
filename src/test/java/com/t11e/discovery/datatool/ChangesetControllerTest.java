package com.t11e.discovery.datatool;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ChangesetControllerTest
{
  private final ChangesetController controller = new ChangesetController();

  @Test
  public void testExceptionDuringCommitedResponse()
    throws XMLStreamException, IOException
  {
    final ChangesetExtractor changesetExtractor = new ChangesetExtractor()
    {
      @Override
      public void writeChangeset(final ChangesetWriter writer, final String changesetType, final Date start,
        final Date end)
      {
        throw new RuntimeException("Here is the root cause");
      }
      @Override
      public String determineType(final Date start)
      {
        return "snapshot";
      }
    };
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    response.setCommitted(true);
    controller.publishImpl(request, response, changesetExtractor, null, null);
    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    final String responseAsString = response.getContentAsString();
    Assert.assertTrue("Response should contain exception notification, but was " + responseAsString,
      responseAsString.contains("*** An exception occured."));
    Assert.assertFalse("Changeset should not be closed, but was " + responseAsString,
      responseAsString.contains("</changeset>"));
  }

  @Test
  public void testExceptionDuringUncommitedResponse()
    throws XMLStreamException, IOException
  {
    final ChangesetExtractor changesetExtractor = new ChangesetExtractor()
    {
      @Override
      public void writeChangeset(final ChangesetWriter writer, final String changesetType, final Date start,
        final Date end)
      {
        throw new RuntimeException("Here is the root cause");
      }

      @Override
      public String determineType(final Date start)
      {
        return "snapshot";
      }
    };
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    try
    {
      controller.publishImpl(request, response, changesetExtractor, null, null);
      Assert.fail("Should thow exception if response is not committed.");
    }
    catch (final RuntimeException e)
    {
      Assert.assertEquals("Here is the root cause", e.getMessage());
    }
  }

}

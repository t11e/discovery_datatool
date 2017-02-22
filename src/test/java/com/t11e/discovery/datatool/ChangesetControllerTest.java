package com.t11e.discovery.datatool;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import junit.framework.Assert;

public class ChangesetControllerTest
{
  private final ChangesetController controller = new ChangesetController();

  @Test
  public void testExceptionDuringCommitedResponse()
    throws Exception
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
    try
    {
      controller.publishImpl(request, response, changesetExtractor, null, null);
      Assert.fail("Expected RuntimeException");
    }
    catch (final RuntimeException e)
    {
      Assert.assertEquals("Here is the root cause", e.getMessage());
    }
    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    final String responseAsString = response.getContentAsString();
    Assert.assertTrue("Response should contain exception notification, but was " + responseAsString,
      responseAsString.contains("*** An exception occured."));
    Assert.assertFalse("Changeset should not be closed, but was " + responseAsString,
      responseAsString.contains("</changeset>"));
  }

  @Test
  public void testExceptionDuringUncommitedResponse()
    throws Exception
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

package com.t11e.discovery.datatool;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ConfigurationController
{
  @Autowired
  private ConfigurationManager configurationManager;

  @RequestMapping(value="/ws/configuration", method=RequestMethod.POST)
  public void setConfiguration(
    final HttpServletRequest request,
    final HttpServletResponse response)
  throws IOException
  {
    configurationManager.loadConfiguration(request.getInputStream());
    response.setStatus(HttpServletResponse.SC_OK);
  }
}

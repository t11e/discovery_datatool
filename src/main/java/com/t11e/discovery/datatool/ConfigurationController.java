package com.t11e.discovery.datatool;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ConfigurationController
{
  private ConfigurableApplicationContext currentContext;
  @RequestMapping(value="/ws/configuration", method=RequestMethod.POST)
  public void setConfiguration(final HttpServletRequest request) throws IOException
  {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    IOUtils.copy(request.getInputStream(), bos);
    final GenericXmlApplicationContext newContext =
      new GenericXmlApplicationContext(new ByteArrayResource(bos.toByteArray()));
    if (currentContext != null)
    {
      currentContext.close();
      currentContext = null;
    }
    currentContext = newContext;
  }
}

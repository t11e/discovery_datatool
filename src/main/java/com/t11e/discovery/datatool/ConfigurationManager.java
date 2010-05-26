package com.t11e.discovery.datatool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationManager
{
  private ConfigurableApplicationContext currentContext;

  public void loadConfiguration(
    final InputStream is)
  throws IOException
  {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    IOUtils.copy(is, bos);
    final GenericXmlApplicationContext newContext =
      new GenericXmlApplicationContext(new ByteArrayResource(bos.toByteArray()));
    newContext.start();
    if (currentContext != null)
    {
      currentContext.stop();
      currentContext.close();
      currentContext = null;
    }
    currentContext = newContext;
  }

  public ChangesetPublisher getChangesetPublisher(final String name)
  {
    ChangesetPublisher result = null;
    if (currentContext != null)
    {
      final ChangesetPublisherManager mgr = currentContext.getBean(ChangesetPublisherManager.class);
      result = mgr.getChangesetPublisher(name);
    }
    return result;
  }
}

package com.t11e.discovery.datatool;

import java.net.BindException;
import java.net.URL;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.util.URIUtil;

public class WebServerMain
{
  public static void main(final String[] args)
  {
    final String resourceName = WebServerMain.class.getName().replace('.', '/') + ".class";
    final URL url = WebServerMain.class.getClassLoader().getResource(resourceName);
    final String jarPath = url.getPath().replaceFirst("^file:", "").replaceFirst("!.*$", "");
    try
    {
      start("8080", jarPath);
    }
    catch (final BindException e)
    {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  public static void start(final String address, final String warPath)
    throws BindException
  {
    final Server server = new Server();
    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    server.setHandler(contexts);

    final SocketConnector connector = new SocketConnector();
    {
      final int colon = address.lastIndexOf(':');
      if (colon < 0)
      {
          connector.setPort(Integer.parseInt(address));
      }
      else
      {
          connector.setHost(address.substring(0,colon));
          connector.setPort(Integer.parseInt(address.substring(colon+1)));
      }
    }
    server.setConnectors(new Connector[]{connector});

    final WebAppContext webapp = new WebAppContext();
    webapp.setWar(warPath);
    webapp.setContextPath(URIUtil.SLASH);
    contexts.addHandler(webapp);

    try
    {
      server.start();
    }
    catch (final BindException e)
    {
      throw e;
    }
    catch (final Exception e)
    {
      throw new RuntimeException(e);
    }
  }
}

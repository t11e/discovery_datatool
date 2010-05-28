package com.t11e.discovery.datatool;

import java.net.URL;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;

public class WebServerMain
{
  public static void main(final String[] args)
  {
    if (args.length != 1)
    {
      System.err.println("Usage: " + WebServerMain.class.getName() + " [address:]port");
      System.exit(1);
    }
    final String address = args[0];
    final String jarPath = getJarPath();
    try
    {
      start(address, jarPath);
    }
    catch (final Exception e)
    {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static String getJarPath()
  {
    final String resourceName = WebServerMain.class.getName().replace('.', '/') + ".class";
    final URL url = WebServerMain.class.getClassLoader().getResource(resourceName);
    final String jarPath = url.getPath().replaceFirst("^file:", "").replaceFirst("!.*$", "");
    return jarPath;
  }

  public static void start(final String address, final String warPath)
    throws Exception
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
    webapp.setContextPath("/");
    webapp.setExtractWAR(false);
    contexts.addHandler(webapp);

    server.start();
  }
}

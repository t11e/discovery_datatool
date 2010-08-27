package com.t11e.discovery.datatool;

import java.io.IOException;
import java.net.URL;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.lang.StringUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

public class WebServerMain
{
  public static void main(final String[] args) throws IOException
  {
    final OptionParser parser = new OptionParser();
    final OptionSpec<String> bindAddress = parser.accepts("bind-address", "Optional address to bind to for listening").withRequiredArg().ofType(String.class);
    final OptionSpec<Integer> port = parser.accepts("port", "The port on which to listen for HTTP requests").withRequiredArg().ofType(Integer.class);;
    final OptionSpec<Integer> httpsPort = parser.accepts("https-port", "The port on which to listen for HTTPS resquests").withRequiredArg().ofType(Integer.class);
    final OptionSpec<String> keystoreFile = parser.accepts("keystore-file", "Java Key Store with combined server certificate and private key for HTTPS use").withRequiredArg().ofType(String.class);
    final OptionSpec<String> keystorePassword = parser.accepts("keystore-pass", "Password for the key store").withRequiredArg().ofType(String.class);
    final OptionSpec<String> keyPassword = parser.accepts("key-pass", "Password for the server key").withRequiredArg().ofType(String.class);
    final OptionSpec<String> truststoreFile = parser.accepts("truststore-file", "Java Key Store with certificates for trusted clients for HTTPS use.").withRequiredArg().ofType(String.class);
    final OptionSpec<String> truststorePassword = parser.accepts("truststore-pass", "Password for the trust store").withRequiredArg().ofType(String.class);

    OptionSet options = null;
    try
    {
      options = parser.parse(args);
    }
    catch (final OptionException e)
    {
      System.err.println(e.getLocalizedMessage());
      parser.printHelpOn(System.err);
      System.exit(1);
    }
    if ((!options.has(port) && !options.has(httpsPort)) ||
        (options.has(httpsPort) && !options.has(keystoreFile)))
    {
      parser.printHelpOn(System.err);
      System.exit(1);
    }
    try
    {
      final WebServerMain main = new WebServerMain(
        options.valueOf(bindAddress),
        options.valueOf(port),
        options.valueOf(httpsPort),
        options.valueOf(keystoreFile),
        options.valueOf(keystorePassword),
        options.valueOf(keyPassword),
        options.valueOf(truststoreFile),
        options.valueOf(truststorePassword)
      );
      main.start();
    }
    catch (final Exception e)
    {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private Server server;

  public WebServerMain(
    final String address,
    final Integer httpPort,
    final Integer httpsPort,
    final String keystoreFile,
    final String keyStorePassword,
    final String keyPassword,
    final String trustStore,
    final String trustStorePassword)
  {
    this(address, httpPort, httpsPort, keystoreFile, keyStorePassword, keyPassword, trustStore, trustStorePassword, null);
  }

  public WebServerMain(
    final String address,
    final Integer httpPort,
    final Integer httpsPort,
    final String keystoreFile,
    final String keyStorePassword,
    final String keyPassword,
    final String trustStore,
    final String trustStorePassword,
    final String warPath)
  {
    init(address, httpPort, httpsPort, keystoreFile, keyStorePassword, keyPassword, trustStore, trustStorePassword,
      warPath != null ? warPath : findWarPath());
  }

  private void init(final String address,
    final Integer httpPort,
    final Integer httpsPort,
    final String keystoreFile,
    final String keyStorePassword,
    final String keyPassword,
    final String trustStore,
    final String trustStorePassword,
    final String warPath)
  {
    server = new Server();
    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    server.setHandler(contexts);

    if (httpPort != null)
    {
      final SocketConnector connector = new SocketConnector();
      connector.setPort(httpPort);
      setAddress(address, connector);
      server.addConnector(connector);
    }
    if (httpsPort != null)
    {
      final SslSocketConnector connector = new SslSocketConnector();
      connector.setPort(httpsPort);
      if (StringUtils.isNotBlank(keystoreFile))
      {
        connector.setKeystore(keystoreFile);
        connector.setPassword(keyStorePassword);
        connector.setKeyPassword(keyPassword);
      }
      if (StringUtils.isNotBlank(trustStore))
      {
        connector.setTruststore(trustStore);
        connector.setTrustPassword(trustStorePassword);
      }
      setAddress(address, connector);
      server.addConnector(connector);
    }

    final WebAppContext webapp = new WebAppContext();
    webapp.setWar(warPath);
    webapp.setContextPath("/");
    contexts.addHandler(webapp);
  }

  private static void setAddress(
    final String address,
    final SocketConnector connector)
  {
    if (StringUtils.isNotBlank(address))
    {
      connector.setHost(address);
    }
  }

  public void start()
    throws Exception
  {
    server.start();
  }

  public void stop()
    throws Exception
  {
    server.stop();
  }

  public void destroy()
  {
    server.destroy();
    server = null;
  }

  private String findWarPath()
  {
    final String resourceName = WebServerMain.class.getName().replace('.', '/') + ".class";
    final URL url = WebServerMain.class.getClassLoader().getResource(resourceName);
    final String warPath = url.getPath().replaceFirst("^file:", "").replaceFirst("!.*$", "");
    return warPath;
  }
}

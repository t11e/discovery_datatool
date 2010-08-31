package com.t11e.discovery.datatool;

import java.io.IOException;
import java.net.URL;
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
    try
    {
      final WebServerMain main = fromArgs(parser, args);
      main.start();
    }
    catch (final Exception e)
    {
      System.err.println(e.getLocalizedMessage());
      parser.printHelpOn(System.err);
      System.exit(1);
    }
  }

  public static WebServerMain fromArgs(final OptionParser parser, final String[] args)
  {
    final OptionSpec<String> bindAddress = parser.accepts("bind-address", "Optional interface address on which the Discovery Data Tool should listen.").withRequiredArg().ofType(String.class);
    final OptionSpec<Integer> port = parser.accepts("port", "Optional port on which the Discovery Data Tool will listen for HTTP requests.").withRequiredArg().ofType(Integer.class);;
    final OptionSpec<Integer> httpsPort = parser.accepts("https-port", "Optional port on which the Discovery Data Tool will listen for HTTPS requests.").withRequiredArg().ofType(Integer.class);
    final OptionSpec<String> keystoreFile = parser.accepts("keystore-file", "Path the a Java Key Store file containing the server certificate and private key. Required if you want to use HTTPS.").withRequiredArg().ofType(String.class);
    final OptionSpec<String> keystorePassword = parser.accepts("keystore-pass", "Password for the Java Key Store file containing the server certificate and private key. Required if you want to use HTTPS.").withRequiredArg().ofType(String.class);
    final OptionSpec<String> keyPassword = parser.accepts("key-pass", "Password for the server private key. Required if your key has a password.").withRequiredArg().ofType(String.class);
    final OptionSpec<String> truststoreFile = parser.accepts("truststore-file", " Optional path to a Java Key Store file containing trusted client certificates.").withRequiredArg().ofType(String.class);
    final OptionSpec<String> truststorePassword = parser.accepts("truststore-pass", "Password for the trust store. Required if using client certificates.").withRequiredArg().ofType(String.class);

    OptionSet options = null;
      options = parser.parse(args);
    if ((!options.has(port) && !options.has(httpsPort)) ||
        (options.has(httpsPort) && (!options.has(keystoreFile) || !options.has(keyPassword) || !options.has(keyPassword))))
    {
      throw new IllegalArgumentException(
        "You must specify --port or --https-port, --keystore-file, --keystore-pass and --key-pass");
    }
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
    return main;
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

package com.t11e.discovery.datatool;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

public class WebServerDaemon
  implements Daemon
{
  private WebServerMain main;

  public void init(final DaemonContext context)
    throws Exception
  {
    final String[] args = context.getArguments();
    if (args.length != 1)
    {
      context.getController().fail("Usage: " + WebServerDaemon.class.getName() + " [address:]port");
    }
    main = new WebServerMain(args[0]);
  }

  public void start()
    throws Exception
  {
    main.start();
  }

  public void stop()
    throws Exception
  {
    main.stop();
  }

  public void destroy()
  {
    main.destroy();
    main = null;
  }
}

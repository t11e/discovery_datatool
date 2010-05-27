package com.t11e.discovery.datatool;

import java.net.URL;

public class WebServerMain
{
  public static void main(final String[] args)
  {
    final String resourceName = WebServerMain.class.getName().replace('.', '/') + ".class";
    final URL url = WebServerMain.class.getClassLoader().getResource(resourceName);
    final String jarPath = url.getPath().replaceFirst("^file:", "").replaceFirst("!.*$", "");
    final String[] webserverArgs = new String[] {
      "8080",
      "-webapp",
      jarPath
    };
    org.mortbay.jetty.Main.main(webserverArgs);
  }
}

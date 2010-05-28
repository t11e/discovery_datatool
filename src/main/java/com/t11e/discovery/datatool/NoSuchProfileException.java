package com.t11e.discovery.datatool;

public class NoSuchProfileException
  extends RuntimeException
{
  private static final long serialVersionUID = 4612076116278889313L;

  public NoSuchProfileException(final String name)
  {
    super("No profile with name: " + name);
  }
}

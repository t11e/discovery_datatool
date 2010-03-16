package com.t11e.discovery.datatool;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component
public class ChangesetExtractor
{
  public void getChangesetForRange(
    final OutputStream os,
    final Date start,
    final Date end)
  {
    try
    {
      os.write("<changeset/>".getBytes());
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
}

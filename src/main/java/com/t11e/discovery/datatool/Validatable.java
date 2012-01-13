package com.t11e.discovery.datatool;

import java.util.Collection;

public interface Validatable
{
  /**
   * Check that the object is valid. Returns validation errors or empty if valid.
   * @param context TODO
   * @return
   */
  Collection<String> checkValid(String context);
}

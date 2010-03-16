package com.t11e.discovery.datatool;

import java.util.Map;

public interface IItemChangesetListener
{
  /**
   * Pseudovalue passed to {@link #onRemoveFromItem(String, Object)} to
   * denote all properties.
   */
  public static final Object ALL = new Object();

  public void onStartChangeset();

  public void onEndChangeset();

  public void onSetItem(String id, Map properties);

  public void onAddToItem(String id, Map properties);

  public void onRemoveFromItem(String id, Object properties);

  public void onAddItem(String id);

  public void onRemoveItem(String id);
}

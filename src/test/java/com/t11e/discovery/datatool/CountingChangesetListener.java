/**
 * 
 */
package com.t11e.discovery.datatool;

import java.util.Map;

final class CountingChangesetListener
  implements IItemChangesetListener
{
  private int setItemCount;
  private int removeItemCount;
  public void onStartChangeset()
  {
  }

  public void onSetItem(final String id, final Map properties)
  {
    ++setItemCount;
  }

  public void onRemoveItem(final String id)
  {
    ++removeItemCount;
  }

  public void onRemoveFromItem(final String id, final Object properties)
  {
  }

  public void onEndChangeset()
  {
  }

  public void onAddToItem(final String id, final Map properties)
  {
  }

  public void onAddItem(final String id)
  {
  }

  public int getRemoveItemCount()
  {
    return removeItemCount;
  }

  public int getSetItemCount()
  {
    return setItemCount;
  }
}
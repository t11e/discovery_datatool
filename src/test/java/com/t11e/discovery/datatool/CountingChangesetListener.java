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
  @Override
  public void onStartChangeset()
  {
  }

  @Override
  public void onSetItem(final String id, final Map<String, Object> properties)
  {
    ++setItemCount;
  }

  @Override
  public void onRemoveItem(final String id)
  {
    ++removeItemCount;
  }

  @Override
  public void onRemoveFromItem(final String id, final Object properties)
  {
  }

  @Override
  public void onEndChangeset()
  {
  }

  @Override
  public void onAddToItem(final String id, final Map<String, Object> properties)
  {
  }

  @Override
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
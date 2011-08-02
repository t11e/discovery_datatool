package com.t11e.discovery.datatool;

import org.springframework.jdbc.core.RowCallbackHandler;

public interface CompletionAwareRowCallbackHandler
  extends RowCallbackHandler
{
  void flushItem();
}

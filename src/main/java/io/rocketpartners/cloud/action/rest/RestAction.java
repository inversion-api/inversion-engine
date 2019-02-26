package io.rocketpartners.cloud.action.rest;

import io.rocketpartners.cloud.model.Action;

public class RestAction<T extends RestAction> extends Action<T>
{
   protected boolean strictRest = false;

   public boolean isStrictRest()
   {
      return strictRest;
   }

   public T withStrictRest(boolean strictRest)
   {
      this.strictRest = strictRest;
      return (T) this;
   }
}

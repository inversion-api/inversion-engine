/**
 * 
 */
package io.rcktapp.rql.dynamo;

import io.rcktapp.rql.Rql;

/**
 * @author tc-rocket
 *
 */
public class DynamoRql extends Rql
{

   static
   {
      Rql.addRql(new DynamoRql());
   }

   private DynamoRql()
   {
      super("dynamo");
      setDoQuote(false);
   }

}

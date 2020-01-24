package io.inversion.cloud.action.cosmosdb;

import io.inversion.cloud.service.spring.InversionApp;

public class MainCosmosApi
{
   public static void main(String[] args) throws Exception
   {
      InversionApp.run(CosmosEngineFactory.engine());
   }
}

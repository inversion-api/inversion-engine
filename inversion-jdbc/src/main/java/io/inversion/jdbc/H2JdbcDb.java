package io.inversion.jdbc;

public class H2JdbcDb extends JdbcDb
{
   public H2JdbcDb()
   {
      super();
   }

   public H2JdbcDb(String name, String driver, String url, String user, String pass, String... ddlUrls)
   {
      super(name, driver, url, user, pass, ddlUrls);
   }

   public H2JdbcDb(String url, String user, String pass)
   {
      super(url, user, pass);
   }

   public H2JdbcDb(String name)
   {
      super(name);
   }

   @Override
   protected void doShutdown()
   {
      try
      {
         try
         {
            String url = getUrl().toUpperCase();
            if (url.indexOf(":MEM:") > 0 && url.indexOf("DB_CLOSE_DELAY=-1") > 0)
            {
               JdbcUtils.execute(getConnection(), "SHUTDOWN");
            }
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
         finally
         {
            super.doShutdown();
         }
      }
      catch (Exception ex)
      {
         //-- ignore
      }
   }

}

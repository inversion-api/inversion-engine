package io.inversion.utils;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import io.inversion.ApiException;

public class Config
{
   static CompositeConfiguration configuration = null;

   private Config()
   {

   }

   public static synchronized Configuration getConfiguration()
   {
      return configuration;
   }

   public static synchronized void setConfiguration(CompositeConfiguration configuration)
   {
      Config.configuration = configuration;
   }

   public static synchronized void clearConfiguration()
   {
      configuration = null;
   }
   
   static synchronized void lazyLoadConfiguration()
   {
      if (configuration == null)
      {
         configuration = loadConfiguration(null, null);
      }
   }

   public static synchronized CompositeConfiguration loadConfiguration(String configPath, String configProfile)
   {
      Configurations configs = new Configurations();
      CompositeConfiguration configuration = new CompositeConfiguration();

      try
      {
         URL url = getResource(".env");
         if (url != null)
         {
            configuration.addConfiguration(configs.properties(url));
         }
         
         configuration.addConfiguration(new SystemConfiguration());
         configuration.addConfiguration(new EnvironmentConfiguration());

         configPath = configPath != null ? configPath : "";

         if (configPath.length() > 0 && !(configPath.endsWith("/") || configPath.endsWith("\\")))
            configPath += "/";

         if (configProfile != null)
         {
            for (int i = 100; i >= -1; i--)
            {
               String fileName = null;

               fileName = configPath + "inversion" + (i < 0 ? "" : i) + "-" + configProfile + ".properties";
               url = getResource(fileName);

               if (url == null)
               {
                  fileName = configPath + "inversion" + "-" + (i < 0 ? "" : i) + "-" + configProfile + ".properties";
                  url = getResource(fileName);
               }
               if (url == null)
               {
                  fileName = configPath + "inversion" + "-" + configProfile + (i < 0 ? "" : i) + ".properties";
                  url = getResource(fileName);
               }
               if (url == null)
               {
                  fileName = configPath + "inversion" + "-" + configProfile + "-" + (i < 0 ? "" : i) + ".properties";
                  url = getResource(fileName);
               }

               if (url != null)
               {
                  configuration.addConfiguration(configs.properties(url));
               }
            }
         }

         for (int i = 100; i >= -1; i--)
         {
            String fileName = configPath + "inversion" + (i < 0 ? "" : i) + ".properties";
            url = getResource(fileName);

            if (url == null)
            {
               fileName = configPath + "inversion" + "-" + (i < 0 ? "" : i) + ".properties";
               url = getResource(fileName);
            }

            if (url != null)
            {
               configuration.addConfiguration(configs.properties(url));
            }
         }
      }
      catch (Exception ex)
      {
         ApiException.throw500InternalServerError(ex);
      }

      Config.configuration = configuration;

      return configuration;
   }

   static URL getResource(String name)
   {
      try
      {
         URL url = null;

         url = Config.class.getClassLoader().getResource(name);
         if (url == null)
         {
            File file = new File(System.getProperty("user.dir"), name);
            if (file.exists())
               url = file.toURI().toURL();
         }

         return url;
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   public static Iterator<String> getKeys()
   {
      lazyLoadConfiguration();
      return configuration.getKeys();
   }

   public static Object getProperty(String key)
   {
      lazyLoadConfiguration();
      return configuration.getProperty(key);
   }

   public static boolean getBoolean(String key)
   {
      lazyLoadConfiguration();
      return configuration.getBoolean(key);
   }

   public static boolean getBoolean(String key, boolean defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getBoolean(key, defaultValue);
   }

   public static Boolean getBoolean(String key, Boolean defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getBoolean(key, defaultValue);
   }

   public static byte getByte(String key)
   {
      lazyLoadConfiguration();
      return configuration.getByte(key);
   }

   public static byte getByte(String key, byte defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getByte(key, defaultValue);
   }

   public static Byte getByte(String key, Byte defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getByte(key, defaultValue);
   }

   public static double getDouble(String key)
   {
      lazyLoadConfiguration();
      return configuration.getDouble(key);
   }

   public static double getDouble(String key, double defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getDouble(key, defaultValue);
   }

   public static Double getDouble(String key, Double defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getDouble(key, defaultValue);
   }

   public static float getFloat(String key)
   {
      lazyLoadConfiguration();
      return configuration.getFloat(key);
   }

   public static float getFloat(String key, float defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getFloat(key, defaultValue);
   }

   public static Float getFloat(String key, Float defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getFloat(key, defaultValue);
   }

   public static int getInt(String key)
   {
      lazyLoadConfiguration();
      return configuration.getInt(key);
   }

   public static int getInt(String key, int defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getInt(key, defaultValue);
   }

   public static Integer getInteger(String key, Integer defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getInteger(key, defaultValue);
   }

   public static long getLong(String key)
   {
      lazyLoadConfiguration();
      return configuration.getLong(key);
   }

   public static long getLong(String key, long defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getLong(key, defaultValue);
   }

   public static Long getLong(String key, Long defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getLong(key, defaultValue);
   }

   public static short getShort(String key)
   {
      lazyLoadConfiguration();
      return configuration.getShort(key);
   }

   public static short getShort(String key, short defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getShort(key, defaultValue);
   }

   public static Short getShort(String key, Short defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getShort(key, defaultValue);
   }

   public static BigDecimal getBigDecimal(String key)
   {
      lazyLoadConfiguration();
      return configuration.getBigDecimal(key);
   }

   public static BigDecimal getBigDecimal(String key, BigDecimal defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getBigDecimal(key, defaultValue);
   }

   public static BigInteger getBigInteger(String key)
   {
      lazyLoadConfiguration();
      return configuration.getBigInteger(key);
   }

   public static BigInteger getBigInteger(String key, BigInteger defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getBigInteger(key, defaultValue);
   }

   public static String getString(String key)
   {
      lazyLoadConfiguration();
      return configuration.getString(key);
   }

   public static String getString(String key, String defaultValue)
   {
      lazyLoadConfiguration();
      return configuration.getString(key, defaultValue);
   }

}

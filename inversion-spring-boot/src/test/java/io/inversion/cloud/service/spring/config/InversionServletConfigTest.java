package io.inversion.cloud.service.spring.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.inversion.cloud.service.Engine;

public class InversionServletConfigTest
{
   @Test
   public void buildServletMapping_noCommonPath()
   {
      Engine e = new Engine().withIncludePaths("/a/b/c", "/d/e/f");
      assertEquals("/*", InversionServletConfig.buildServletMapping(e));
   }

   @Test
   public void buildServletMapping_partialCommonPath()
   {
      Engine e = new Engine().withIncludePaths("/a/b/c", "/a/b/f");
      assertEquals("/a/b/*", InversionServletConfig.buildServletMapping(e));
   }

   @Test
   public void buildServletMapping_breakOnOptional()
   {
      Engine e = new Engine().withIncludePaths("/a/b/c", "/a/b/[c]");
      assertEquals("/a/b/*", InversionServletConfig.buildServletMapping(e));
   }

   @Test
   public void buildServletMapping_breakOnVariable()
   {
      Engine e = new Engine().withIncludePaths("/a/b/c", "/a/b/:c");
      assertEquals("/a/b/*", InversionServletConfig.buildServletMapping(e));
   }

   @Test
   public void buildServletMapping_breakOnRegex()
   {
      Engine e = new Engine().withIncludePaths("/a/b/c", "/a/b/{c}");
      assertEquals("/a/b/*", InversionServletConfig.buildServletMapping(e));
   }
}

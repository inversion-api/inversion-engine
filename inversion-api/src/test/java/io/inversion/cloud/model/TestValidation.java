package io.inversion.cloud.model;

import org.junit.Test;

import io.inversion.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestValidation extends TestCase
{
   @Test
   public void testValidation1()
   {
      Request req = new Request("GET", "http://localhost:8080?someField1=value1", Utils.read(getClass().getResourceAsStream("testValidation1.json")));

      assertEquals("tester", req.validate("username").required().asString());
      assertEquals(5, req.validate("amount").minMax(1, 10).asInt());

      req.validate("missingField")//
         .in("value1", "value2");//not an error because it is not .required()

      req.validate("someField1")//
         .required()//
         .in("value1", "value2")//
         .out("value5", 2, 4)//
         .matches("value[0-9]")//
         .matches("val.*");

      req.validate("amount").minMax(1, 10)//
         .ge(5)//
         .gt(3)//
         .le(5)//
         .lt(8)//
         .in("5")//
         .in(5)//
         .out(7, 9, 10, "asdf");

      req.validate("$.book.price").minMax(1, 100);
      req.validate("$.book.price").eq(50.00);
      Double price = req.validate("$.book.price").eq("50").asDouble();

      boolean failed = false;

      try
      {
         failed = false;
         assertEquals(5, req.validate("amount").minMax(8, 10).asInt());
      }
      catch (Exception ex)
      {
         failed = true;
      }
      finally
      {
         if (!failed)
            fail("validation shoud have failed");
      }

      try
      {
         failed = false;
         req.validate("someField1").required().in("value3", "value4");
      }
      catch (Exception ex)
      {
         failed = true;
      }
      finally
      {
         if (!failed)
            fail("validation shoud have failed");
      }

      try
      {
         failed = false;
         req.validate("someField1").required().in("value3", "value4");
      }
      catch (Exception ex)
      {
         failed = true;
      }
      finally
      {
         if (!failed)
            fail("validation shoud have failed");
      }

      try
      {
         failed = false;
         req.validate("someField1").required().out("value1");
      }
      catch (Exception ex)
      {
         failed = true;
      }
      finally
      {
         if (!failed)
            fail("validation shoud have failed");
      }
   }

}

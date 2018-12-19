package io.rcktapp.rql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.forty11.j.J;

public class TestParts
{

   @Test
   public void test1()
   {
      String sql = "SELECT DISTINCT e.dayId, b.receiptTime, var1 AS MobileNumber, var2 AS PointStatus, var3 AS QualifyingItemsInBasket, l.locationcode, l.division, l.region, l.market, p.playercode FROM event e" + // 
            " JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId" + //
            " JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId" + // 
            " JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid" + //
            " JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid" + // 
            " WHERE category = 'LOYALTY' AND event = 'JUUL'" + // 
            " AND   b.voided = false" + //
            " AND   li.voided = false" + //
            " ORDER BY b.receipttime ASC" + //
            " LIMIT 50";

      Parts parts = new Parts(sql);

      sql = "SELECT DISTINCT e.dayId, b.receiptTime, var1 AS MobileNumber, var2 AS PointStatus, var3 AS QualifyingItemsInBasket, l.locationcode, l.division, l.region, l.market, p.playercode";
      equals(sql, parts.select);

      sql = "FROM event e" + //
            " JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId" + //
            " JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId" + // 
            " JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid" + //
            " JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid";
      equals(sql, parts.from);

      sql = "WHERE category = 'LOYALTY' AND event = 'JUUL'" + // 
            " AND   b.voided = false" + //
            " AND   li.voided = false";
      equals(sql, parts.where);

      sql = "";
      assertNull(parts.group);

      sql = "ORDER BY b.receipttime ASC";
      equals(sql, parts.order);

      sql = "LIMIT 50";
      equals(sql, parts.limit);

   }

   void equals(String str1, String str2)
   {
      if (str1 == null || str2 == null)
      {
         assertEquals(str1, str2);
      }
      else
      {
         str1 = str1.replaceAll("\\s+", " ").trim();
         str2 = str2.replaceAll("\\s+", " ").trim();
         
         if (!J.equal(str1, str2))
            compare(str1, str2);

         assertEquals(str1.trim(), str2.trim());
      }
   }

   public static boolean compare(String str1, String str2)
   {
      str1 = str1.replaceAll("\\s+", " ").trim();
      str2 = str2.replaceAll("\\s+", " ").trim();

      if (!str1.equals(str2))
      {
         str2 = str2.replaceAll("SQL_CALC_FOUND_ROWS ", "");
         str2 = str2.replaceAll(" LIMIT 0, 100", "");

         str1 = str1.replaceAll("SQL_CALC_FOUND_ROWS ", "");
         str1 = str1.replaceAll(" LIMIT 0, 100", "");

         if (!str1.equals(str2))
         {
            System.out.println("\r\n");
            System.out.println("\r\n");
            System.out.println(str1);
            System.out.println(str2);

            for (int i = 0; i < str1.length() && i < str2.length(); i++)
            {
               if (str1.charAt(i) == str2.charAt(i))
               {
                  System.out.print(" ");
               }
               else
               {
                  System.out.println("X");
                  break;
               }
            }
            System.out.println(" ");

            String err = "failed test: " + str1 + " != " + str2;
            return false;
         }
      }
      return true;
   }

   @Test
   public void test2()
   {
      String sql = "SELECT DISTINCT e.dayId, b.receiptTime, var1 AS MobileNumber, var2 AS PointStatus, var3 AS QualifyingItemsInBasket, l.locationcode, l.division, l.region, l.market, p.playercode FROM event e" + // 
            " JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId" + //
            " JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId" + // 
            " JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid" + //
            " JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid" + // 
            " WHERE category = 'LOYALTY' AND event = 'JUUL'" + //
            " AND   b.voided = false" + //
            " AND   li.voided = false" + //
            " ORDER BY b.receipttime ASC" + // 
            " LIMIT 50";

      Parts parts = new Parts(sql);

      sql = " SELECT DISTINCT e.dayId, b.receiptTime, var1 AS MobileNumber, var2 AS PointStatus, var3 AS QualifyingItemsInBasket, l.locationcode, l.division, l.region, l.market, p.playercode";
      equals(sql.trim(), parts.select);

      sql = " FROM event e" + //
            " JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId" + // 
            " JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId" + // 
            " JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid" + //
            " JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid";
      equals(sql, parts.from);

      sql = " WHERE category = 'LOYALTY' AND event = 'JUUL'" + //
            " AND   b.voided = false" + //
            " AND   li.voided = false";
      equals(sql, parts.where);

      sql = "";
      assertNull(parts.group);

      sql = " ORDER BY b.receipttime ASC";
      equals(sql, parts.order);

      sql = " LIMIT 50";
      equals(sql, parts.limit);

   }

   @Test
   public void test3()
   {
      String sql = "SELECT DISTINCT e.dayId, b.receiptTime, var1 AS MobileNumber, var2 AS PointStatus, var3 AS QualifyingItemsInBasket, l.locationcode, l.division, l.region, l.market, p.playercode FROM event e\n" + // 
            "\n" + //
            "JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId\n" + // 
            "\n" + //
            "JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId\n" + //
            "\n" + //
            "JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid\n" + // 
            "\n" + // 
            "JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid\n" + // 
            "\n" + //
            "WHERE category = 'LOYALTY' AND event = 'JUUL'\n" + // 
            "\n" + //
            "AND   b.voided = false\n" + //
            "\n" + //
            "AND   li.voided = false\n" + //
            "\n" + //
            "ORDER BY b.receipttime ASC\n" + //
            "\n" + //
            "LIMIT 50";

      Parts parts = new Parts(sql);

      sql = "SELECT DISTINCT e.dayId, b.receiptTime, var1 AS MobileNumber, var2 AS PointStatus, var3 AS QualifyingItemsInBasket, l.locationcode, l.division, l.region, l.market, p.playercode";
      equals(sql, parts.select);

      sql = " FROM event e" + //
            "\n" + //
            "JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId\n" + // 
            "\n" + //
            "JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId\n" + //
            "\n" + //
            "JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid\n" + // 
            "\n" + // 
            "JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid\n";
      equals(sql, parts.from);

      sql = "\n" + //
            "WHERE category = 'LOYALTY' AND event = 'JUUL'\n" + // 
            "\n" + //
            "AND   b.voided = false\n" + //
            "\n" + //
            "AND   li.voided = false\n";
      equals(sql, parts.where);

      sql = "";
      assertNull(parts.group);

      sql = "\n" + //
            "ORDER BY b.receipttime ASC\n";
      equals(sql, parts.order);

      sql = "\n" + //
            "LIMIT 50";
      equals(sql, parts.limit);

   }

}

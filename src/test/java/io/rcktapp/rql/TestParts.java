package io.rcktapp.rql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

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
      assertEquals(sql, parts.select);

      sql = "FROM event e" + //
            " JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId" + //
            " JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId" + // 
            " JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid" + //
            " JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid";
      assertEquals(sql, parts.from);

      sql = "WHERE category = 'LOYALTY' AND event = 'JUUL'" + // 
            " AND   b.voided = false" + //
            " AND   li.voided = false";
      assertEquals(sql, parts.where);

      sql = "";
      assertNull(parts.group);

      sql = "ORDER BY b.receipttime ASC";
      assertEquals(sql, parts.order);

      sql = "LIMIT 50";
      assertEquals(sql, parts.limit);

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
      assertEquals(sql, parts.select);

      sql = " FROM event e" + //
            " JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId" + // 
            " JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId" + // 
            " JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid" + //
            " JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid";
      assertEquals(sql, parts.from);

      sql = " WHERE category = 'LOYALTY' AND event = 'JUUL'" + //
            " AND   b.voided = false" + //
            " AND   li.voided = false";
      assertEquals(sql, parts.where);

      sql = "";
      assertNull(parts.group);

      sql = " ORDER BY b.receipttime ASC";
      assertEquals(sql, parts.order);

      sql = " LIMIT 50";
      assertEquals(sql, parts.limit);

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
      assertEquals(sql, parts.select);

      sql = " FROM event e" + //
            "\n" + //
            "JOIN Basket b ON e.basketId = b.id AND e.dayId = b.dayId AND e.locationId = b.locationId\n" + // 
            "\n" + //
            "JOIN LineItem li ON li.basketId = b.id AND li.dayId = b.dayId AND li.locationId = b.locationId\n" + //
            "\n" + //
            "JOIN location l ON  l.id = e.locationid AND l.orgid = e.orgid\n" + // 
            "\n" + // 
            "JOIN player p ON p.locationid = e.locationid AND p.id = e.playerid AND p.orgid = e.orgid\n";
      assertEquals(sql, parts.from);

      sql = "\n" + //
            "WHERE category = 'LOYALTY' AND event = 'JUUL'\n" + // 
            "\n" + //
            "AND   b.voided = false\n" + //
            "\n" + //
            "AND   li.voided = false\n";
      assertEquals(sql, parts.where);

      sql = "";
      assertNull(parts.group);

      sql = "\n" + //
            "ORDER BY b.receipttime ASC\n";
      assertEquals(sql, parts.order);

      sql = "\n" + //
            "LIMIT 50";
      assertEquals(sql, parts.limit);

   }

}

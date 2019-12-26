package io.inversion.cloud.model;

import io.inversion.cloud.utils.Utils;

public class Validation
{
   Object value              = null;
   String customErrorMessage = null;
   String propOrPath         = null;

   public Validation(Request req, String propOrPath, String customErrorMessage)
   {
      value = req.getParam(propOrPath);
      if (value == null && req.getJson() != null)
         value = req.getJson().find(propOrPath);

      this.propOrPath = null;
      this.customErrorMessage = customErrorMessage;
   }

   public Validation(Response res, String jsonPath, String customErrorMessage)
   {
      this.value = res.find(jsonPath);
      this.propOrPath = null;
      this.customErrorMessage = customErrorMessage;
   }

   public Validation required()
   {
      if (value == null)
         fail("Requird field '" + propOrPath + "' is missing.");

      return this;
   }

   public Validation matches(String regex)
   {
      if (value == null)
         return this;

      if (!value.toString().matches(regex))
         fail("Field '" + propOrPath + "' does not match the required pattern.");

      return this;
   }

   public Validation in(Object... possibleValues)
   {
      if (value == null)
         return this;

      if (!Utils.in(value, possibleValues))
         fail("Field '" + propOrPath + "' is not one of the possible values.");

      return this;
   }

   public Validation out(Object... excludedValues)
   {
      if (value == null)
         return this;

      if (Utils.in(value, excludedValues))
         fail("Field '" + propOrPath + "' has a restricted value.");

      return this;
   }

   protected int compareTo(Object compareTo)
   {
      Object value = this.value;

      if (compareTo instanceof Number)
      {
         try
         {
            value = Double.parseDouble(value.toString());
            compareTo = Double.parseDouble(compareTo.toString());
         }
         catch (Exception ex)
         {
            //ignore numeric type conversion error.
         }
      }

      return ((Comparable) value).compareTo(compareTo);
   }

   public Validation gt(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) < 1)
         fail("Filed '" + propOrPath + "' is less than the required value.");

      return this;
   }

   public Validation ge(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) < 0)
         fail("Filed '" + propOrPath + "' is less than the required value.");

      return this;
   }

   public Validation lt(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) > -1)
         fail("Filed '" + propOrPath + "' is greater than the required value.");

      return this;
   }

   public Validation le(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) > 0)
         fail("Filed '" + propOrPath + "' is greater than the required value.");

      return this;
   }

   public Validation eq(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) != 0)
         fail("Filed '" + propOrPath + "' is not equal to the required value.");

      return this;
   }

   public Validation ne(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) != 0)
         fail("Filed '" + propOrPath + "' is equal to a restricted value.");

      return this;
   }

   public Validation length(int max)
   {
      if (value == null)
         return this;

      if (value.toString().length() > max)
         fail("Field '" + propOrPath + "' is longer than the max allowed length of '" + max + "'.");

      return this;
   }

   public Validation length(int min, int max)
   {
      if (value == null)
         return this;

      int length = value.toString().length();

      if (length > max)
         fail("Field '" + propOrPath + "' is longer than the maximum allowed length of '" + max + "'.");

      if (length < min)
         fail("Field '" + propOrPath + "' is shorter than the minimum allowed length of '" + max + "'.");

      return this;
   }

   public Validation minMax(Number min, Number max)
   {
      if (value == null)
         return this;

      max(max);
      min(min);
      return this;
   }

   public Validation max(Number max)
   {
      if (value == null)
         return this;

      if (Double.parseDouble(max.toString()) < Double.parseDouble(value.toString()))
         fail("Field '" + propOrPath + "' is greater than the required maximum of '" + max + "'.");

      return this;
   }

   public Validation min(Number min)
   {
      if (value == null)
         return this;

      if (Double.parseDouble(min.toString()) > Double.parseDouble(value.toString()))
         fail("Field '" + propOrPath + "' is less than the required minimum of '" + min + "'.");

      return this;
   }

   public Object value()
   {
      return value;
   }

   public JSNode asNode()
   {
      if (value == null)
         return null;

      if (value instanceof String)
         value = JSNode.parseJson(value.toString());

      return ((JSNode) value);
   }

   public JSArray asArray()
   {
      if (value == null)
         return null;

      if (value instanceof String)
         value = JSNode.parseJsonArray(value.toString());

      return ((JSArray) value);
   }

   public String asString()
   {
      if (value == null)
         return null;

      return value.toString();
   }

   public int asInt()
   {
      if (value == null)
         return -1;

      try
      {
         return Integer.parseInt(value + "");
      }
      catch (Exception ex)
      {
         fail("Field '" + propOrPath + "' must be an integer.");
      }

      return -1;
   }

   public double asDouble()
   {
      if (value == null)
         return -1;

      try
      {
         return Double.parseDouble(value + "");
      }
      catch (Exception ex)
      {
         fail("Field '" + propOrPath + "' must be an number.");
      }

      return -1;
   }

   public boolean asBoolean()
   {
      try
      {
         return Boolean.parseBoolean(value + "");
      }
      catch (Exception ex)
      {
         fail("Field '" + propOrPath + "' must be a boolean.");
      }

      return false;
   }

   protected void fail(String defaultErrorMessage)
   {
      String message = customErrorMessage != null ? customErrorMessage : defaultErrorMessage;
      throw new ApiException(SC.SC_400_BAD_REQUEST, message);
   }
}

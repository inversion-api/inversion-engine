package io.inversion.cloud.model;

import io.inversion.cloud.utils.Utils;

/**
 * Utility designed to make it easy to validate request properties or request body
 * json values while you are retrieving them.  
 *
 * <h3>Required (Not Null)</h3>
 *
 * To ensure a field is not null, use the required() method:
 * <ul>
 *     <li>String nameFirst = request.validate("nameFirst", "A first name is required").required().asString();</li>
 * </ul>
 *
 * <h3>Comparison</h3>
 *
 * To validate a number is greater than 5, then return its value:
 * <ul>
 * <li>int myParam = request.validate("myParamName", "optional_custom_error_message").gt(5).asInt();
 * </ul>
 *
 * @see Request#validate(String)
 * @see Request#validate(String,String)
 * @author wells
 */
public class Validation
{
   Object value              = null;
   String customErrorMessage = null;
   String propOrPath         = null;

   public Validation(Request req, String propOrPath, String customErrorMessage)
   {
      value = req.getUrl().getParam(propOrPath);
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

   /**
    * If there are any <code>childProps</code> they must exist on the JSNode
    * found at <code>pathOrProp</code>.  If <code>childProps</code> are null/empty
    * then  <code>pathOrProp</code> must not be null.
    * 
    * @return
    * @throws ApiException 400 if the referenced validation is null.
    */
   public Validation required(String... childProps)
   {
      if (Utils.empty(value))
         fail("Required field '" + propOrPath + "' is missing.");

      if (childProps != null && value instanceof JSNode && !((JSNode) value).isArray())
      {
         for (String childProp : childProps)
         {
            if (Utils.empty(((JSNode) value).get(childProp)))
            {
               fail("Required field '" + propOrPath + "." + childProp + "' is missing.");
            }
         }
      }

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
         fail("Field '" + propOrPath + "' is less than the required value.");

      return this;
   }

   public Validation ge(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) < 0)
         fail("Field '" + propOrPath + "' is less than the required value.");

      return this;
   }

   public Validation lt(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) > -1)
         fail("Field '" + propOrPath + "' is greater than the required value.");

      return this;
   }

   public Validation le(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) > 0)
         fail("Field '" + propOrPath + "' is greater than the required value.");

      return this;
   }

   public Validation eq(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) != 0)
         fail("Field '" + propOrPath + "' is not equal to the required value.");

      return this;
   }

   public Validation ne(Object compareTo)
   {
      if (value == null)
         return this;

      if (compareTo(compareTo) != 0)
         fail("Field '" + propOrPath + "' is equal to a restricted value.");

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

   /**
    * Throws an ApiException 400 using the provided custom error message.  If a custom error message
    * was not provided, a default error message is utilized.
    * @param defaultErrorMessage
    * @throws ApiException
    */
   protected void fail(String defaultErrorMessage)
   {
      String message = customErrorMessage != null ? customErrorMessage : defaultErrorMessage;
      ApiException.throw400BadRequest(message);
   }
}

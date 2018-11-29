/**
 * 
 */
package io.rcktapp.rql.dynamo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.rcktapp.api.Index;
import io.rcktapp.api.Table;
import io.rcktapp.api.handler.dynamo.DynamoDb;
import io.rcktapp.rql.Predicate;

/**
 * @author tc-rocket
 *
 */
public class DynamoExpression
{

   public static Map<String, String> OPERATOR_MAP = new HashMap<>();
   public static Map<String, String> FUNCTION_MAP = new HashMap<>();
   static
   {
      OPERATOR_MAP.put("eq", "=");
      OPERATOR_MAP.put("ne", "<>");
      OPERATOR_MAP.put("gt", ">");
      OPERATOR_MAP.put("ge", ">=");
      OPERATOR_MAP.put("lt", "<");
      OPERATOR_MAP.put("le", "<=");

      FUNCTION_MAP.put("w", "contains");
      FUNCTION_MAP.put("sw", "begins_with");
      FUNCTION_MAP.put("nn", "attribute_exists");
      FUNCTION_MAP.put("n", "attribute_not_exists");
   }

   Table                  table;

   int                    fieldCnt           = 0;
   int                    argCnt             = 0;
   StringBuilder          buffer             = new StringBuilder();
   Map<String, String>    fields             = new LinkedHashMap<>();
   Map<String, Object>    args               = new LinkedHashMap<>();

   Map<String, Predicate> excludedPredicates = new HashMap<>();

   String                 sortField;
   String                 sortDirection;
   Index                  index;

   public DynamoExpression(Table table)
   {
      super();
      this.table = table;
   }

   String nextFieldName()
   {
      fieldCnt++;
      return "#name" + fieldCnt;
   }

   String nextArgName()
   {
      argCnt++;
      return ":val" + argCnt;
   }

   public static boolean isKnownOperator(String operator)
   {
      return OPERATOR_MAP.containsKey(operator);
   }

   public static boolean isKnownFunction(String func)
   {
      return FUNCTION_MAP.containsKey(func);
   }

   public void appendOperatorExpression(String token, String field, String val)
   {
      String fieldName = nextFieldName();
      String argName = nextArgName();
      buffer.append(fieldName + " " + OPERATOR_MAP.get(token) + " " + argName);
      fields.put(fieldName, field);
      args.put(argName, DynamoDb.cast(val, field, table));
   }

   public void appendFunctionExpression(String token, String field, String val)
   {
      if (val != null)
      {
         String fieldName = nextFieldName();
         String argName = nextArgName();
         buffer.append(FUNCTION_MAP.get(token) + "(" + fieldName + ", " + argName + ")");
         fields.put(fieldName, field);
         args.put(argName, DynamoDb.cast(val, field, table));
      }
      else
      {
         String fieldName = nextFieldName();
         buffer.append(FUNCTION_MAP.get(token) + "(" + fieldName + ")");
         fields.put(fieldName, field);
      }
   }

   public void appendSpaces(int depth)
   {
      buffer.append(spaces(depth));
   }

   public void append(String s)
   {
      buffer.append(s);
   }

   public String buildExpression()
   {
      return buffer.toString();
   }

   public String spaces(int depth)
   {
      String s = "";
      for (int i = 0; i < depth; i++)
      {
         s = s + "  ";
      }
      return s;
   }

   public Map<String, Object> getArgs()
   {
      return args;
   }

   public Map<String, String> getFields()
   {
      return fields;
   }

   public Table getTable()
   {
      return table;
   }

   public void addExcludedPredicate(String name, Predicate pred)
   {
      this.excludedPredicates.put(name, pred);
   }

   public Predicate getExcludedPredicate(String name)
   {
      return this.excludedPredicates.get(name);
   }

   /**
    * This is used when the user is trying to sort using a Local Secondary Index
    */
   public void setSortIndexInformation(String sortField, String sortDirection, Index index)
   {
      this.sortField = sortField;
      this.sortDirection = sortDirection;
      this.index = index;
   }

   public String getSortField()
   {
      return sortField;
   }

   public String getSortDirection()
   {
      return sortDirection;
   }

   public Index getIndex()
   {
      return index;
   }

}

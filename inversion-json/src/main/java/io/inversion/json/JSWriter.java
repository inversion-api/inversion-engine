package io.inversion.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IdentityHashMap;

public class JSWriter {

//    public static String toJson(Object obj){
//        if(obj instanceof JSNode){
//            return toJson((JSNode)obj);
//        }
//        try{
//            return JSNode.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
//        }
//        catch(Exception ex){
//            if(ex instanceof RuntimeException)
//                throw ((RuntimeException)ex);
//            throw new RuntimeException("Error writing json: " + ex.getMessage(), ex);
//        }
//    }

    static String toJson(JSNode node){
        return toJson(node, true, false);
    }

    static String toJson(JSNode node, boolean pretty, boolean lowercasePropertyNames) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonGenerator         json = new JsonFactory().createGenerator(baos);
            if (pretty)
                json.useDefaultPrettyPrinter();

            private_writeObject(node, json, new IdentityHashMap<>(), lowercasePropertyNames, "#");
            json.flush();
            baos.flush();

            return new String(baos.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static void private_writeObject(JSNode object, JsonGenerator json, IdentityHashMap<Object, String> visited, boolean lowercaseNames, String path) throws Exception {

        if (visited.containsKey(object)) {
            json.writeStartObject();
            json.writeStringField("$ref", visited.get(object));
            json.writeEndObject();
            return;
        }

        visited.put(object, path);

        if (object.isList()) {
            private_writeArrayNode(((JSList) object), json, visited, lowercaseNames, path);
            return;
        }

        json.writeStartObject();

        //json.writeStringField("$id", path);

        for (JSProperty p : object.getProperties()) {
            String name  = lowercaseNames ? p.getKey().toString().toLowerCase() : p.getKey().toString();
            Object value = p.getValue();

            if (value == null) {
                json.writeNullField(name);
            } else if (value instanceof JSNode) {
                json.writeFieldName(name);
                private_writeObject((JSNode) value, json, visited, lowercaseNames, path + "/" + name);
            } else if (value instanceof String) {
                if (value.equals("null"))
                    json.writeNullField(name);
                else
                    json.writeStringField(name, (String) value);
            } else if (value instanceof Boolean) {
                json.writeBooleanField(name, (Boolean) value);
            } else if (value instanceof Integer) {
                json.writeNumberField(name, (Integer) value);
            } else if (value instanceof Long) {
                json.writeNumberField(name, (Long) value);
            } else if (value instanceof Float) {
                json.writeNumberField(name, (Float) value);
            } else if (value instanceof Double) {
                json.writeNumberField(name, (Double) value);
            } else if (value instanceof BigInteger) {
                json.writeNumberField(name, ((BigInteger) value).intValue());
            } else if (value instanceof BigDecimal) {
                json.writeNumberField(name, (BigDecimal) value);
            } else if (value instanceof Date) {
                String dateFormat = "yyyy-MM-dd'T'HH:mmZ";
                SimpleDateFormat f = new SimpleDateFormat(dateFormat);
                String dateString = f.format((Date) value);
                json.writeStringField(name, dateString);
            } else {
                String strVal = value + "";
                if ("null".equals(strVal)) {
                    json.writeNullField(name);
                } else {
                    strVal = encodeStringValue(strVal);
                    json.writeStringField(name, strVal);
                }
            }
        }
        json.writeEndObject();
    }

    static void private_writeArrayNode(JSList array, JsonGenerator json, IdentityHashMap<Object, String> visited, boolean lowercaseNames, String path) throws Exception {
        json.writeStartArray();
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if (value == null) {
                json.writeNull();
            } else if (value instanceof JSNode) {
                private_writeObject((JSNode) value, json, visited, lowercaseNames, path + "[" + i + "]");
            } else {
                if (value instanceof String) {
                    if (value.equals("null"))
                        json.writeNull();
                    else
                        json.writeString(value.toString());
                } else if (value instanceof Boolean) {
                    json.writeBoolean((Boolean) value);
                } else if (value instanceof Integer) {
                    json.writeNumber((Integer) value);
                } else if (value instanceof Long) {
                    json.writeNumber((Long) value);
                } else if (value instanceof Float) {
                    json.writeNumber((Float) value);
                } else if (value instanceof Double) {
                    json.writeNumber((Double) value);
                } else if (value instanceof BigInteger) {
                    json.writeNumber((BigInteger) value);
                } else if (value instanceof BigDecimal) {
                    json.writeNumber((BigDecimal) value);
                } else if (value instanceof Date) {
                    String dateFormat = "yyyy-MM-dd'T'HH:mmZ";
                    SimpleDateFormat f = new SimpleDateFormat(dateFormat);
                    String dateString = f.format((Date) value);
                    json.writeString(dateString);
                } else {
                    json.writeString(encodeStringValue(value.toString()));
                }
            }
        }
        json.writeEndArray();
    }

    /**
     * Replaces JSON control characters with spaces.
     *
     * @param str the string to encode
     * @return str with control characters replaced with spaces
     * @see <a href="https://stackoverflow.com/questions/14028716/how-to-remove-control-characters-from-java-string">How to remove control characters from java Strings</a>
     */
    public static String encodeStringValue(String str) {
        if (str == null)
            return null;

        str = str.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}\u00A0&&[^\r\n\t]]", " ");
        return str;
    }
}

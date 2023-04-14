package io.inversion.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inversion.utils.Utils;

import java.io.InputStream;

public class JSParser {
    public static JsonFactory  parserFactory = new JsonFactory();
    public static ObjectMapper objectMapper  = new ObjectMapper();

    public static <T> T parseJson(String json, Class<T> clazz) {
        try {
            return (T) objectMapper.readValue(json.getBytes(), clazz);

        } catch (Exception e) {
            throw Utils.ex("Invalid JSON.", e);
        }
    }

    public static <T> T parseJson(InputStream json, Class<T> clazz) {
        try {
            return (T) objectMapper.readValue(json, clazz);

        } catch (Exception e) {
            throw Utils.ex("Invalid JSON.", e);
        }
    }

    public static <T> T parseJson(JSNode json, Class<T> clazz) {
        return parseJson(json.toString(false), clazz);
    }

    public static Object asJson(Object object) {
        try {
            String json = objectMapper.writer().writeValueAsString(object);
            return parseJson(json);
        } catch (Exception e) {
            throw Utils.ex("Invalid JSON.", e);
        }
    }


    /**
     * Turns a JSON string in to JSNode (maps), JSList (lists), String numbers and booleans.
     * <p>
     * Jackson is the underlying parser
     *
     * @param json the json string to parse
     * @return a String, number, boolean, JSNode or JSList
     */
    public static Object parseJson(String json) {
        try {
            JsonParser parser = parserFactory.createParser(json.getBytes());
            return parseJson(parser, null);

        } catch (Exception e) {
            throw Utils.ex("Invalid JSON.", e);
        }
    }

    public static Object parseJson(InputStream json) {
        try {
            JsonParser parser = parserFactory.createParser(json);
            return parseJson(parser, null);

        } catch (Exception e) {
            throw Utils.ex("Invalid JSON.");
        }
    }

    public static JSNode parseJson(JsonParser parser, JSNode node) throws Exception {
        JsonToken token;
        String    name = null;

        if (node == null) {
            JsonToken startToken = parser.currentToken();
            if (startToken != null) {
                if (startToken == JsonToken.START_OBJECT)
                    node = new JSMap();
                else if (startToken == JsonToken.START_ARRAY)
                    node = new JSList();
            }
        }

        boolean isArray = node != null && node.isList();

        while (true) {

            token = parser.nextToken();
            //System.out.println(token + " -     " + parser.getCurrentName() + " - " + parser.getValueAsString());

            if (token == null)
                return node;

            switch (token) {

                case END_OBJECT:
                case END_ARRAY:
                    return node;
                case START_OBJECT:
                    JSNode childNode = parseJson(parser, new JSMap());
                    if (node == null)
                        return childNode;
                    if (isArray)
                        ((JSList) node).add(childNode);
                    else
                        node.put(name, childNode);
                    break;
                case START_ARRAY:
                    JSNode childArr = parseJson(parser, new JSList());
                    if (node == null)
                        return childArr;
                    if (isArray)
                        ((JSList) node).add(childArr);
                    else
                        node.put(name, childArr);
                    break;
                case FIELD_NAME:
                    name = parser.getCurrentName();
                    break;
                case VALUE_EMBEDDED_OBJECT:
                    break;
                case VALUE_STRING:
                    if (isArray)
                        ((JSList) node).add(parser.getValueAsString());
                    else
                        node.put(name, parser.getValueAsString());
                    break;
                case VALUE_NUMBER_INT:
                    try {
                        if (isArray)
                            ((JSList) node).add(parser.getValueAsInt());
                        else
                            node.put(name, parser.getValueAsInt());
                        break;
                    } catch (InputCoercionException ex) {
                        if (isArray)
                            ((JSList) node).add(parser.getValueAsLong());
                        else
                            node.put(name, parser.getValueAsLong());
                        break;
                    }

                case VALUE_NUMBER_FLOAT:
                    if (isArray)
                        ((JSList) node).add(parser.getValueAsDouble());
                    else
                        node.put(name, parser.getValueAsDouble());
                    break;
                case VALUE_TRUE:
                    if (isArray)
                        ((JSList) node).add(true);
                    else
                        node.put(name, true);
                    break;
                case VALUE_FALSE:
                    if (isArray)
                        ((JSList) node).add(false);
                    else
                        node.put(name, false);
                    break;
                case VALUE_NULL:
                    if (isArray)
                        ((JSList) node).add(null);
                    else
                        node.put(name, null);
                    break;

                default:
                    throw Utils.ex("Unknown token {}", token);
            }
        }
    }

    /**
     * Utility overloading of {@link JSParser#parseJson(String)} to cast the return as a JSNode
     *
     * @param json the json string to parse
     * @return the result of parsing the json document cast to a JSNode
     * @throws ClassCastException if the result of parsing is not a JSNode
     */
    public static JSNode asJSNode(String json) throws ClassCastException {
        return ((JSNode) parseJson(json));
    }

    public static JSNode asJSNode(InputStream in) {
        return ((JSNode) parseJson(in));
    }

    public static JSMap asJSMap(String json) throws ClassCastException {
        return ((JSMap) parseJson(json));
    }

    public static JSMap asJSMap(InputStream in) {
        return ((JSMap) parseJson(in));
    }

    /**
     * Utility overloading of {@link JSParser#parseJson(String)} to cast the return as a JSList
     *
     * @param json a json string containing an as the root element
     * @return the result of parsing the json document cast to a JSList
     * @throws ClassCastException if the result of parsing is not a JSList
     */
    public static JSList asJSList(String json) {
        return ((JSList) parseJson(json));
    }

    public static JSList asJSList(InputStream in) {
        return ((JSList) parseJson(in));
    }


}

package io.inversion.config;

import io.inversion.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

public class Decoder {

    static final Logger log = LoggerFactory.getLogger(Decoder.class);

    /**
     * Sorts based on the number of "." characters first and then
     * based on the string value.
     *
     * @param keys the keys to sort
     * @return the sorted list of keys
     */
    public static List<String> sort(Collection<String> keys) {
        List<String> sorted = new ArrayList<>(keys);
        sorted.sort((o1, o2) -> {
            int count1 = o1.length() - o1.replace(".", "").length();
            int count2 = o2.length() - o2.replace(".", "").length();
            if (count1 != count2)
                return count1 > count2 ? 1 : -1;

            return o1.compareTo(o2);
        });

        return sorted;
    }

    /**
     * Three step process
     * 1. Instantiate all beans
     * 2. Set primitive types on all beans
     * 3. Set object types on all beans
     */
    public LinkedHashMap<String, String> decode(Context context, Map<String, String> propsToDecode) {
        try{
            return decode0(context, propsToDecode);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    LinkedHashMap<String, String> decode0(Context context, Map<String, String> propsToDecode)throws Exception {

        LinkedHashMap<String, String> applied = new LinkedHashMap<>();
        TreeMap<String, String> sortedPropsToDecode = new TreeMap<>(propsToDecode);

        HashMap<String, Map> loaded = new LinkedHashMap();

        //FIRST STEP
        // - instantiate all beans

        for (Object p : propsToDecode.keySet()) {
            String key = (String) p;

            if (key.endsWith(".class") || key.endsWith(".className")) {
                String name = key.substring(0, key.lastIndexOf("."));
                String cn   = (String) propsToDecode.get(key);

                if (context.hasName(name))
                    //throw new ApiException("Your configuration declared a class with a name that already exists '{} = {}'", key, cn);
                    throw new RuntimeException("Your configuration declared a class with a name that already exists '{} = {}'");//, key, cn);

                try {
                    Object obj       = Class.forName(cn).getDeclaredConstructor().newInstance();
                    Field  nameField = Utils.getField("name", obj.getClass());
                    if (nameField != null)
                        nameField.set(obj, name);

                    applied.put(key, cn);
                    context.putBean(name, obj);
                } catch (Exception ex) {
                    System.err.println("Error instantiating class: '" + cn + "'");
                    throw new RuntimeException(ex);
                }

                loaded.put(name, new HashMap<>());
            }
        }

        List<String> keys = new ArrayList(context.getNames());
        keys = sort(keys);

        //LOOP THROUGH TWICE.
        // - First loop, set atomic props
        // - Second loop, set bean props

        for (int i = 0; i <= 1; i++) {
            boolean isFirstPassSoLoadOnlyPrimitives = i == 0;

            for (String beanName : keys) {
                Object bean     = context.getBean(beanName);
                List   beanKeys = getKeys(beanName, sortedPropsToDecode);
                for (Object p : beanKeys) {
                    String key = (String) p;

                    if (key.endsWith(".class") || key.endsWith(".className"))
                        continue;

                    //make sure this only has a single "."
                    if ((key.startsWith(beanName + ".") && key.lastIndexOf(".") == beanName.length())) {
                        String prop     = key.substring(key.lastIndexOf(".") + 1);
                        String valueStr = propsToDecode.get(key);

                        if (valueStr != null)
                            valueStr = valueStr.trim();

                        //if (value != null && (value.length() == 0 || "null".equals(value)))
                        if ("null".equalsIgnoreCase(valueStr)) {
                            valueStr = null;
                        }

                        Object value = valueStr;
                        if (!Utils.empty(valueStr) && context.hasName(valueStr)) {
                            value = context.getBean(valueStr);
                        }

                        boolean valueIsBean = (!(value == null || valueStr.equals("") || valueStr.equals("null"))
                                && (context.hasName(valueStr) || context.hasName((Utils.explode(",", valueStr).get(0)))));

                        if (isFirstPassSoLoadOnlyPrimitives && valueIsBean) {
                            continue;
                        } else if (!isFirstPassSoLoadOnlyPrimitives && !valueIsBean) {
                            continue;
                        }

                        Field field = Utils.getField(prop, bean.getClass());
                        if (field != null) {
                            applied.put(key, valueStr);
                            Class type = field.getType();
                            if (value == null || type.isAssignableFrom(value.getClass())) {
                                if (value == null) {
                                    if (valueStr == null || valueStr.trim().equals("") || valueStr.trim().toLowerCase().equals("null"))
                                        field.set(bean, null);
                                    else
                                        log.warn("Unable to determine value for property '{} = {}'", prop, valueStr);
                                } else {
                                    field.set(bean, value);
                                }
                            } else if (Collection.class.isAssignableFrom(type)) {
                                Collection list = (Collection) context.cast(key, valueStr, type, field);
                                ((Collection) field.get(bean)).addAll(list);
                            } else if (Map.class.isAssignableFrom(type)) {
                                Map map = (Map) context.cast(key, valueStr, type, null);
                                ((Map) field.get(bean)).putAll(map);
                            } else {
                                field.set(bean, context.cast(key, valueStr, type, null));
                            }
                        } else {
                            log.warn("Can't map property: " + Context.maskOutput(key, value + ""));
                        }
                    }
                }
            }
        }
        return applied;
    }

    List<String> getKeys(String beanName, TreeMap<String, String> props) {
        Set<String> keys       = new HashSet<>();
        String      beanPrefix = beanName + ".";
        Set<String> keySet     = props.tailMap(beanPrefix).keySet();
        for (String key : keySet) {
            if (!key.startsWith(beanPrefix)) {
                break;
            }
            if (!(key.endsWith(".class") || key.endsWith(".className"))) {
                if (!keys.contains(beanName))
                    keys.add(key);
            }
        }
        return new ArrayList(keys);
    }

    protected Object cast0(String str) {
        if ("true".equalsIgnoreCase(str))
            return true;

        if ("false".equalsIgnoreCase(str))
            return true;

        if (str.matches("\\d+")) {
            try {
                return Integer.parseInt(str);
            } catch (Exception ex) {
                try {
                    return Long.parseLong(str);
                } catch (Exception ex2) {
                    //OK must be a really huge number
                }
            }
        }
        return str;
    }
}

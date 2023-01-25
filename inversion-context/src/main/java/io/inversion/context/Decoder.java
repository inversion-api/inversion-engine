package io.inversion.context;

import io.inversion.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
        try {
            return decode0(context, propsToDecode);
        } catch (Exception ex) {
            throw Utils.ex(ex);
        }
    }

    public Object decode(Context context, Type type, String encoded) {
        Class clazz = type instanceof Class ? (Class)type : (Class)((ParameterizedType)type).getRawType();
        Codec codec = context.getCodec(clazz);
        if (codec != null)
            return codec.decode(context, type, encoded);

        Object bean = context.getBean(encoded);
        if (bean == null)
            throw Utils.ex("Unable to find a codec or value for {}", encoded);

        return bean;
    }

    LinkedHashMap<String, String> decode0(Context context, Map<String, String> propsToDecode) throws Exception {

        LinkedHashMap<String, String> applied             = new LinkedHashMap<>();
        TreeMap<String, String>       sortedPropsToDecode = new TreeMap<>(propsToDecode);

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

        List<String> beanNames = new ArrayList(context.getNames());
        beanNames = sort(beanNames);

        for (String beanName : beanNames) {
            Object           bean            = context.getBean(beanName);
            List<FieldToSet> propertiesToSet = getFieldsToSet(bean, beanName, sortedPropsToDecode);
            for (FieldToSet propToSet : propertiesToSet) {

                Field  field    = propToSet.getField();
                Class  clazz    = field.getType();
                Type   type     = field.getGenericType();
                String strValue = propToSet.getStringVal();

                if (context.getCodec(clazz) !=null){
                    Object value = null;
                    if (strValue != null)
                        value = context.getCodec(clazz).decode(context, type, strValue);
                    propToSet.getField().set(bean, value);
                    applied.put(propToSet.getKey(), propToSet.getStringVal());
                }
                else if(context.getBean(strValue) != null){
                    propToSet.getField().set(bean, context.getBean(strValue));
                    applied.put(propToSet.getKey(), propToSet.getStringVal());
                }
                else{
                    throw Utils.ex("Unable to decode property {}", propToSet.getKey());
                }
            }
        }
        return applied;
    }


    public List<FieldToSet> getFieldsToSet(Object bean, String beanName, TreeMap<String, String> propsToDecode) {
        List<FieldToSet> propertiesToSet = new ArrayList<>();
        List<String>     keys            = getKeys(beanName, propsToDecode);
        for (String key : keys) {
            if (key.endsWith(".class") || key.endsWith(".className"))
                continue;

            String strValue  = propsToDecode.get(key);
            String fieldName = key.substring(key.lastIndexOf(".") + 1);

            if (strValue != null)
                strValue = strValue.trim();

            if ("null".equalsIgnoreCase(strValue))
                strValue = null;

            Field field = Utils.getField(fieldName, bean.getClass());
            if (field == null){
                log.debug("Skipping unknown bean property: '" + beanName + "." + fieldName + "'");
                continue;
            }
            propertiesToSet.add(new FieldToSet(bean, key, field, strValue));
        }
        return propertiesToSet;
    }


    protected Class getArrayElementClass(Class arrayClass) {
        try {
            Class  subtype;
            String typeStr = arrayClass.toString();

            if (typeStr.startsWith("class [Z")) {
                subtype = boolean.class;
            } else if (typeStr.startsWith("class [B")) {
                subtype = byte.class;
            } else if (typeStr.startsWith("class [C")) {
                subtype = char.class;
            } else if (typeStr.startsWith("class [I")) {
                subtype = int.class;
            } else if (typeStr.startsWith("class [J")) {
                subtype = long.class;
            } else if (typeStr.startsWith("class [F")) {
                subtype = float.class;
            } else if (typeStr.startsWith("class [D")) {
                subtype = double.class;
            } else //if (typeStr.startsWith("class ["))
            {
                subtype = Class.forName(typeStr.substring(typeStr.indexOf("[") + 2, typeStr.indexOf(";")));
            }
            return subtype;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    protected List<String> getKeys(String beanName, TreeMap<String, String> props) {
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

    class FieldToSet {
        Object bean      = null;
        String key       = null;
        Field  field     = null;
        String stringVal = null;

        public FieldToSet(Object bean, String key, Field field, String stringVal) {
            this.bean = bean;
            this.key = key;
            this.field = field;
            this.stringVal = stringVal;
        }

        public Object getBean() {
            return bean;
        }

        public FieldToSet withBean(Object bean) {
            this.bean = bean;
            return this;
        }

        public String getKey() {
            return key;
        }

        public FieldToSet withKey(String key) {
            this.key = key;
            return this;
        }

        public Field getField() {
            return field;
        }

        public FieldToSet withField(Field field) {
            this.field = field;
            return this;
        }

        public String getStringVal() {
            return stringVal;
        }

        public FieldToSet withStringVal(String strVal) {
            this.stringVal = strVal;
            return this;
        }
    }

}

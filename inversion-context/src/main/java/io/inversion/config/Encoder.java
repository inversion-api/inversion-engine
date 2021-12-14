package io.inversion.config;

import ioi.inversion.utils.Utils;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

class Encoder {

    static final Logger log = LoggerFactory.getLogger(Encoder.class);
    static MultiKeyMap<Object, Object> defaults      = new MultiKeyMap();

    List<Field>                 excludeFields = new ArrayList<>();
    List                        excludeTypes  = Utils.asList(Logger.class, Context.class);

    /**
     * Encoding an object of this type will simply involve calling toString().
     */
    private static final Set<Class<?>> STRINGIFIED_TYPES = new HashSet<>();

    static {
        STRINGIFIED_TYPES.add(Boolean.class);
        STRINGIFIED_TYPES.add(Character.class);
        STRINGIFIED_TYPES.add(Byte.class);
        STRINGIFIED_TYPES.add(Short.class);
        STRINGIFIED_TYPES.add(Integer.class);
        STRINGIFIED_TYPES.add(Long.class);
        STRINGIFIED_TYPES.add(Float.class);
        STRINGIFIED_TYPES.add(Double.class);
        STRINGIFIED_TYPES.add(Void.class);
        STRINGIFIED_TYPES.add(String.class);
    }


    public Encoder() {

    }

    public LinkedHashMap<String, String> encode(Context context, Object... beans) {
        Set<Object>             encoded = new HashSet();
        LinkedHashMap<String, String> props   = new LinkedHashMap();
        for (Object bean : beans) {
            encode0(context, bean, props, encoded);
        }

        return props;
    }


    public String encode0(Context context, Object bean, LinkedHashMap<String, String> props, Set<Object> encoded) {
        try {
            if (bean == null)
                return null;

            Codec codec = context.getCodec(bean.getClass());
            if(codec != null)
                return codec.toString(bean);

            if (STRINGIFIED_TYPES.contains(bean.getClass()))
                return bean + "";

            if (encoded.contains(bean))
                return context.makeName(bean);

            encoded.add(bean); //recursion guard

            final String name = context.makeName(bean);


//            System.out.println(bean.getClass().getName() + " -> " + name);

//            if (bean.getClass().isArray())
//                bean = Utils.asList(bean);
//
//            if (bean instanceof Collection && ((Collection)bean).size() > 0) {
//                List values = new ArrayList<>();
//                for (Object child : ((Collection) bean)) {
//                    String childKey = encode0(context, child, props, encoded);
//                    values.add(childKey);
//                }
//                String encodedProp = Utils.implode(",", values);
//                props.put(name + ".values", encodedProp);
//            }
//            else if(bean instanceof Map && ((Map)bean).size() > 0){
//                Map map = (Map)bean;
//                for (Object mapKey : map.keySet()) {
//                    String encodedKey   = encode0(context, mapKey, props, encoded);
//                    String encodedValue = encode0(context, map.get(mapKey), props, encoded);
//                    props.put(name + "." + encodedKey, encodedValue);
//                }
//            }


            List<Field> fields = Utils.getFields(bean.getClass());

            if (!defaults.containsKey(bean.getClass())) {
                Object clean = null;
                try {
                    for (Field field : fields) {
                        if (!includeField(context, field))
                            continue;

                        if (clean == null)
                            clean = bean.getClass().getDeclaredConstructor().newInstance();

                        try {
                            Object defaultValue   = field.get(clean);
                            String encodedDefault = encode0(context, defaultValue, props, encoded);
                            defaults.put(bean.getClass(), field.getName(), encodedDefault);
                        } catch (Exception ex) {
                            log.debug("Unable to determine default value for {}: ", field, ex);
                            Object defaultValue = field.get(clean);
                        }
                    }
                } catch (NoSuchMethodException ex) {
                    //-- probably no empty constructor
                    //-- put this here so future encoders won't try to load defaults
                    defaults.put(bean.getClass(), "__none", "__none");
                }
            }

            for (Field field : fields) {
                if (!includeField(context, field))
                    continue;

                System.out.println("TRAVERSING: " + field);

                Object value = field.get(bean);

                if (value != null) {


                    String fieldKey = name + "." + field.getName();

                    if (value.getClass().isArray())
                        value = Utils.asList(value);

                    codec = context.getCodec(value.getClass());
                    if (codec != null)
                        value = codec.toString(value);


                    if (value instanceof Collection) {
                        if (((Collection) value).size() == 0)
                            continue;

                        List values = new ArrayList<>();
                        for (Object child : ((Collection) value)) {
                            String childKey = encode0(context, child, props, encoded);
                            values.add(childKey);
                        }

                        String encodedProp = values.toString();
                        Object defaultProp = defaults.get(bean.getClass(), field.getName());

                        if (!Utils.equal(encodedProp, defaultProp))
                            props.put(fieldKey, encodedProp);

                    } else if (value instanceof Map) {
                        Map map = (Map) value;
                        if (map.size() == 0)
                            continue;

                        TreeMap values = new TreeMap();
                        for (Object mapKey : map.keySet()) {
                            String encodedKey   = encode0(context, mapKey, props, encoded);
                            String encodedValue = encode0(context, map.get(mapKey), props, encoded);
                            values.put(encodedKey, encodedValue);
                        }

                        String encodedProp = values.toString();
                        Object defaultProp = defaults.get(bean.getClass(), field.getName());

                        if (!Utils.equal(encodedProp, defaultProp))
                            props.put(fieldKey, encodedProp);

                    } else {
                        if (STRINGIFIED_TYPES.contains(value.getClass())) {
                            Object defaultVal = defaults.get(bean.getClass(), field.getName());
                            if (defaultVal != null && defaultVal.equals(value))
                                continue;
                        } else if (!includeField(context, field))
                            continue;

                        String encodedProp = encode0(context, value, props, encoded);
                        Object defaultProp = defaults.get(bean.getClass(), field.getName());

                        if (!Utils.equal(encodedProp, defaultProp))
                            props.put(fieldKey, encodedProp);
                    }
                }
            }

            return name;
        } catch (Exception ex) {
            Context.dump("PROPERTIES THROUGH ERROR:", props);
            throw Utils.ex(ex, "Error encoding class {}", bean.getClass().getName());
        }
    }


    boolean includeField(Context context, Field field) {

        if (excludeField(field))
            return false;

        if (context.getCodec(field.getType()) != null)
            return true;

        Class c = field.getType();
        if (Collection.class.isAssignableFrom(c)) {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                if (pt.getActualTypeArguments()[0] instanceof TypeVariable) {
                    //can't figure out the type so consider it important
                    return true;
                } else if (pt.getActualTypeArguments()[0] instanceof ParameterizedType) {
                    //TODO: is this the right decision
                    return false;
                }

                c = (Class) pt.getActualTypeArguments()[0];
            }

            boolean inc = !excludeType(c);
            return inc;
        } else if (Properties.class.isAssignableFrom(c)) {
            return true;
//        } else if (JSNode.class.isAssignableFrom(c)) {
//            return true;
        } else if (Map.class.isAssignableFrom(c)) {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                if (!(pt.getActualTypeArguments()[0] instanceof Class)
                        || !(pt.getActualTypeArguments()[1] instanceof Class)) {
                    return false;
                }

                Class keyType   = (Class) pt.getActualTypeArguments()[0];
                Class valueType = (Class) pt.getActualTypeArguments()[1];

                return !excludeType(keyType) && !excludeType(valueType);
            } else {
                throw Utils.ex("You need to parameterize this object: {}" + field);
            }
        } else {
            boolean inc = !excludeType(c);
            return inc;
        }
    }


    boolean excludeField(Field field) {

        if (field.getName().equals("name"))//this is implicit in the property key
            return true;

        if (field.getName().indexOf("$") > -1)
            return true;

        if (Modifier.isStatic(field.getModifiers()))
            return true;

        if (Modifier.isTransient(field.getModifiers()))
            return true;

        if (Modifier.isPrivate(field.getModifiers()))
            return true;

        if (excludeFields.contains(field) || excludeTypes.contains(field.getType()))
            return true;

        return false;
    }

    boolean excludeType(Class type) {
        boolean exclude = excludeTypes.contains(type);
        if (!exclude && type.getName().indexOf("org.springframework") > -1)
            exclude = true;

        return exclude;
    }
}

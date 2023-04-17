package io.inversion.context;

import io.inversion.context.codec.PrimitiveCodec;
import io.inversion.context.codec.ToStringCodec;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class Encoder {

    static final Logger                      log      = LoggerFactory.getLogger(Encoder.class);
    static       MultiKeyMap<Object, String> defaults = new MultiKeyMap();

    protected Includer includer = new Includer();

    public Encoder() {

    }

    public LinkedHashMap<String, String> encode(Context context, Object... beans) {
        Set<Object>                   encoded = new HashSet();
        LinkedHashMap<String, String> props   = new LinkedHashMap();
        for (Object bean : beans) {
            encode0(context, new CodecPath(null, null, null, bean), props, encoded);
        }
        return props;
    }


    public String encode0(Context context, CodecPath codecPath, LinkedHashMap<String, String> props, Set<Object> encoded) {

        //System.out.println("encode0: " + codecPath);

        Object bean = codecPath.getBean();

        try {
            if (bean == null)
                return null;

            if (bean.getClass().isEnum()) {
                String val = bean.toString();
                return val;
            }

            Codec codec = context.getCodec(bean.getClass());
            if (codec != null)
                return codec.encode(context, codecPath, props, encoded);

            if (encoded.contains(bean))
                return context.makeName(bean);
            encoded.add(bean); //recursion guard

            final String name = context.makeName(bean);

            props.put(name + ".class", bean.getClass().getName());

            List<Field> fields = Utils.getFields(bean.getClass());
            if (!defaults.containsKey(bean.getClass())) {
                Object clean = null;

                try {
                    clean = bean.getClass().getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    defaults.put(bean.getClass(), "__none", "__none");
                }

                if (clean != null) {
                    for (Field field : fields) {
                        if (!includer.includeField(context, field))
                            continue;

                        try {
                            Object defaultValue = field.get(clean);
                            if (defaultValue != null) {
                                Codec defaultCodec = context.getCodec(defaultValue.getClass());
                                if (defaultCodec != null && defaultCodec instanceof ToStringCodec) {
                                    String encodedDefault = ((ToStringCodec) defaultCodec).toString(defaultValue);
                                    defaults.put(bean.getClass(), field.getName(), encodedDefault);
                                } else {
                                    if (defaultValue instanceof Map && ((Map) defaultValue).size() == 0) {
                                        defaults.put(bean.getClass(), field.getName(), "{}");
                                    } else if (defaultValue instanceof Collection && ((Collection) defaultValue).size() == 0) {
                                        defaults.put(bean.getClass(), field.getName(), "[]");
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            log.error("Unable to determine default value for {}: ", field, ex);
                            Object defaultValue = field.get(clean);
                        }
                    }
                }
            }

            for (Field field : fields) {
                if (!includer.includeField(context, field))
                    continue;

                Object fieldValue = field.get(bean);
                if (fieldValue != null) {
                    String fieldKey    = name + "." + field.getName();
                    String encodedProp = encode0(context, new CodecPath(codecPath, field.getGenericType(), field.getName(), fieldValue), props, encoded);
                    Object defaultProp = defaults.get(bean.getClass(), field.getName());
                    if (!Utils.equal(encodedProp, defaultProp))
                        props.put(fieldKey, encodedProp);
                }
            }

            return name;
        } catch (Throwable ex) {
            ex.printStackTrace();
            log.error("Error encoding bean path: " + codecPath, ex);
            Context.dump("PROPERTIES THROUGH ERROR:", props);
            ex = Utils.getCause(ex);
            throw Utils.ex(ex, "Error encoding class {}", bean.getClass().getName());
        }
    }


    public Includer getIncluder() {
        return includer;
    }

    public void setIncluder(Includer includer) {
        this.includer = includer;
    }
}

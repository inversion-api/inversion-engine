package io.inversion.context;

import java.lang.reflect.Type;
import java.util.*;

public interface Codec {

    Object decode(Context context, Type type, String encoded);

    default String encode(Context context, CodecPath codecPath, LinkedHashMap<String, String> props, Set<Object> encoded) {
        return codecPath.getBean() + "";
    }

    default List<Class> getTypes(){
        return new ArrayList<>();
    };

}

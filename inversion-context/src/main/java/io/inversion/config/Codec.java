package io.inversion.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Codec{

    List<Class> types = new ArrayList();

    public Codec(){
    }

    public Codec(Class... classes){
        withTypes(classes);
    }

    public Codec withTypes(Class... classes){
        for(int i=0; classes != null && i< classes.length;i++){
            Class clazz = classes[i];
            if(clazz != null && !this.types.contains(clazz))
                this.types.add(clazz);
        }
        return this;
    }

    public List<Class> getTypes(){
        return Collections.unmodifiableList(types);
    }

    public String toString(Object object){
        return object.toString();
    }
    public abstract Object fromString(Class clazz, String encoded);
}

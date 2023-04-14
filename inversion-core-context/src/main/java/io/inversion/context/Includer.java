/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.inversion.context;

import io.inversion.utils.Utils;
import org.slf4j.Logger;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Includer {

    Set<String> excludePackages = new LinkedHashSet<>();
    Set<Field>  excludeFields   = new LinkedHashSet<>();
    Set<Class>  excludeClasses  = Utils.add(new LinkedHashSet<>(), Logger.class, Context.class);


    public boolean includeBean(Object bean) {
        return !exclude(bean.getClass());
    }

    public boolean includeField(Context context, Field field) {
        if (exclude(field))
            return false;

        if (context.getCodec(field.getType()) != null)
            return true;

        Class type = field.getType();

        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            Type genericType = field.getGenericType();
            return includeType(genericType);
        }
        return true;
    }


    private boolean includeType(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt    = (ParameterizedType) genericType;
            Type[]            types = pt.getActualTypeArguments();
            for (int i = 0; types != null && i < types.length; i++) {
                if (!includeType(types[0]))
                    return false;
            }
            return true;
        } else if (genericType instanceof TypeVariable) {
            //can't figure out the type so consider it important
            return true;
        } else if (genericType instanceof Class) {
            return !exclude((Class) genericType);
        } else {
            Context.log.warn("Unsupported Type " + genericType.getTypeName());
            return false;
        }
    }


    private boolean exclude(Field field) {

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

        if (excludeFields.contains(field))
            return true;

        if (exclude(field.getType()))
            return true;

        return false;
    }

    private boolean exclude(Class type) {
        //if(type.getName().indexOf("$") > 0)
        //    return true;

        boolean exclude = excludeClasses.contains(type);
        if (!exclude) {
            String packageName = type.getName();
            if (packageName.lastIndexOf(".") > 0)
                packageName = packageName.substring(0, packageName.lastIndexOf("."));
            for (String excludedPackage : excludePackages) {
                if (packageName.matches(excludedPackage)) {
                    exclude = true;
                    break;
                }
            }
        }

        return exclude;
    }

    public Includer withExcludePackages(String... packages){
        for(int i=0; packages != null && i<packages.length; i++){
            if(packages[i] != null && packages[i].length() > 0)
                this.excludePackages.add(packages[i]);
        }
        return this;
    }

    public Set<String> getExcludePackages() {
        return new LinkedHashSet<>(excludePackages);
    }

    public void setExcludePackages(Set<String> excludePackages) {
        this.excludePackages.clear();
        if (excludePackages != null)
            this.excludePackages.addAll(excludePackages);
    }

    public Set<Field> getExcludeFields() {
        return new LinkedHashSet<>(excludeFields);
    }

    public void setExcludeFields(Set<Field> excludeFields) {
        this.excludeFields.clear();
        if (excludeFields != null)
            this.excludeFields.addAll(excludeFields);
    }

    public Includer withExcludePackages(Field... fields){
        for(int i=0; fields != null && i<fields.length; i++){
            if(fields[i] != null)
                this.excludeFields.add(fields[i]);
        }
        return this;
    }

    public Set<Class> getExcludeClasses() {
        return new LinkedHashSet<>(excludeClasses);
    }

    public void setExcludeClasses(Set<Class> excludeClasses) {
        this.excludeClasses.clear();
        if (excludeClasses != null)
            this.excludeClasses.addAll(excludeClasses);
    }

    public Includer withExcludePackages(Class... classes){
        for(int i=0; classes != null && i<classes.length; i++){
            if(classes[i] != null)
                this.excludeClasses.add(classes[i]);
        }
        return this;
    }
}

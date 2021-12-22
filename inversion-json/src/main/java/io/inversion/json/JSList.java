package io.inversion.json;

import io.inversion.utils.Utils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.*;
import java.util.stream.Stream;

public class JSList<T extends Object> extends JSNode implements List<T> {

    ArrayList<Object> elements = new ArrayList<>();

    public JSList(Object... objects) {
        if (objects != null && objects.length == 1 && objects[0].getClass().isArray()) {
            objects = (Object[]) objects[0];
        } else if (objects != null && objects.length == 1 && java.util.Collection.class.isAssignableFrom(objects[0].getClass())) {
            objects = ((java.util.Collection) objects[0]).toArray();
        }

        for (int i = 0; objects != null && i < objects.length; i++)
            add(objects[i]);
    }

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- JSBase Implementations

    @Override
    protected JSProperty getProperty(Object key) {
        int idx = Utils.atoi(key);
        if(idx < 0)
            return null;
        return new JSProperty(idx, get(idx));
    }


    @Override
    protected List<JSProperty> getProperties() {
        List<JSProperty> properties = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            properties.add(new JSProperty(i, elements.get(i)));
        }
        return properties;
    }

    @Override
    protected JSProperty putProperty(JSProperty property) {
        int    idx = Utils.atoi(property.getKey());
        if(idx < 0)
            throw Utils.ex("JSList can not be indexed with key '{}'", idx);
        Object old = get(idx);
        set(idx, property.getValue());
        return old != null ? new JSProperty(idx, old) : null;
    }

    @Override
    protected boolean removeProperty(JSProperty property) {
        int idx = Utils.atoi(property.getValue());
        if(idx < 0)
            return false;
        if (idx < size()) {
            remove(idx);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        elements.clear();
    }

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- List Implementations

    @Override
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Override
    public Iterator iterator() {
        return elements.iterator();
    }

    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        return elements.toArray(a);
    }

    @Override
    public boolean add(Object o) {
        return elements.add(o);
    }

    @Override
    public boolean remove(Object o) {
        return elements.remove(o);
    }

    @Override
    public boolean containsAll(Collection c) {
        return elements.containsAll(c);
    }

    @Override
    public boolean addAll(Collection c) {
        return elements.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection c) {
        return elements.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection c) {
        return elements.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection c) {
        return elements.retainAll(c);
    }

    @Override
    public T get(int index) {
        if (index > -1 && index >= elements.size())
            return null;
        return (T)elements.get(index);
    }

    @Override
    public Object set(int index, Object element) {
        while (elements.size() < index + 1)
            elements.add(null);

        return elements.set(index, element);
    }

    @Override
    public void add(int index, Object element) {
        while (elements.size() < index)
            elements.add(null);

        elements.add(index, element);
    }

    @Override
    public T remove(int index) {
        return (T)elements.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return elements.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return elements.lastIndexOf(o);
    }

    @Override
    public ListIterator listIterator() {
        return elements.listIterator();
    }

    @Override
    public ListIterator listIterator(int index) {
        return elements.listIterator(index);
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        return elements.subList(fromIndex, toIndex);
    }

    @Override
    public Stream stream(){
        return elements.stream();
    }
}

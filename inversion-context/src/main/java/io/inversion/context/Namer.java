package io.inversion.context;

public interface Namer {
    default String name(Context context, Object object){return null;}
}

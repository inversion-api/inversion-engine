package io.inversion.config;

public interface Namer {
    default String name(Context context, Object bean){return null;};
}

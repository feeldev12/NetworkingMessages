package me.feeldev.networking.serialization;

import me.feeldev.networking.serialization.serializers.ICustomSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SerializerRegistry {
    public static final SerializerRegistry INSTANCE = new SerializerRegistry();

    private final Map<Class<?>, ICustomSerializer<?>> serializers = new HashMap<>();

    private SerializerRegistry() {
    }

    public <T> void register(Class<T> clazz, ICustomSerializer<T> serializer) {
        serializers.put(clazz, serializer);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ICustomSerializer<T>> get(Class<T> clazz) {
        return Optional.ofNullable((ICustomSerializer<T>) serializers.get(clazz));
    }
}

package me.feeldev.networking.serialization.serializers;

import me.feeldev.networking.serialization.FriendlyByteBuf;

public interface ICustomSerializer<T> {
    /**
     * Escribe el objeto en el buffer.
     * @param buf El buffer donde escribir.
     * @param object El objeto a escribir.
     */
    void write(FriendlyByteBuf buf, T object);

    /**
     * Lee del buffer y crea un nuevo objeto.
     * @param buf El buffer desde donde leer.
     * @return Una nueva instancia del objeto.
     */
    T read(FriendlyByteBuf buf);
}

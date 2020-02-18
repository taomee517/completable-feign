package com.demo.client.feign;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;

public final class CodecProvider {

    private CodecProvider() {}

    private static final Map<Class<? extends Encoder>, Encoder> encoders = new ConcurrentHashMap<>();
    private static final Map<Class<? extends Decoder>, Decoder> decoders = new ConcurrentHashMap<>();

    private static <E extends Encoder> E loadEncoder(final Class<E> encoderClass) {
        final ServiceLoader encoderLoader = ServiceLoader.load(encoderClass);
        final Iterator<E> encoders = encoderLoader.iterator();
        return encoders.hasNext() ? encoders.next() : null;
    }

    private static <D extends Decoder> D loadDecoder(final Class<D> decoderClass) {
        final ServiceLoader decoderLoader = ServiceLoader.load(decoderClass);
        final Iterator<D> decoders = decoderLoader.iterator();
        return decoders.hasNext() ? decoders.next() : null;
    }

    public static <E extends Encoder> E getEncoder(final Class<E> encoderClass) {
        return (E) encoders.computeIfAbsent(encoderClass, key -> loadEncoder(encoderClass));
    }

    public static <D extends Decoder> D getDecoder(final Class<D> decoderClass) {
        return (D) decoders.computeIfAbsent(decoderClass, key -> loadDecoder(decoderClass));
    }

    public static <C extends Encoder & Decoder> C getCoder(final Class<C> coderClass) {
        return (C) decoders.computeIfAbsent(coderClass, key -> getEncoder(coderClass));
    }

    public static <C extends Encoder & Decoder, B extends Feign.Builder> Feign.Builder configureCoder(
            final Feign.Builder feignBuilder, final Class<C> coderClass) {
        configureDecoder(feignBuilder, coderClass);
        return configureEncoder(feignBuilder, coderClass);
    }

    public static Feign.Builder configureDecoder(final Feign.Builder feignBuilder,
                                                 final Class<? extends Decoder> decoderClass) {
        final Decoder decoder = getDecoder(decoderClass);
        return decoder == null ? feignBuilder : feignBuilder.decoder(decoder);
    }

    public static Feign.Builder configureEncoder(final Feign.Builder feignBuilder,
                                                 final Class<? extends Encoder> encoderClass) {
        final Encoder encoder = getEncoder(encoderClass);
        return encoder == null ? feignBuilder : feignBuilder.encoder(encoder);
    }
}

package com.demo.client.test;

import java.io.IOException;
import java.lang.reflect.Type;

import com.demo.client.feign.Codec;
import feign.FeignException;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.EncodeException;
import feign.codec.Encoder;

public class TestCodec implements Codec {

    private final Encoder encoder = new Encoder.Default();
    private final Decoder decoder = new Decoder.Default();

    @Override
    public Object decode(final Response response, final Type type)
            throws IOException, FeignException {
        return decoder.decode(response, type);
    }

    @Override
    public void encode(final Object object, final Type bodyType, final RequestTemplate template)
            throws EncodeException {
        encoder.encode(object, bodyType, template);
    }
}


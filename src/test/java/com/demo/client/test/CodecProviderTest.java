package com.demo.client.test;

import com.demo.client.feign.Codec;
import com.demo.client.feign.CodecProvider;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CodecProviderTest {

    @Test
    public void testGetCodec() {
        final Codec codec = CodecProvider.getCoder(Codec.class);
        assertEquals(TestCodec.class, codec.getClass());
    }

    @Test
    public void testGetEncoder() {
        final Encoder encodec = CodecProvider.getEncoder(Encoder.class);
        assertEquals(Encoder.Default.class, encodec.getClass());
    }

    @Test
    public void testGetDecoder() {
        final Decoder decodec = CodecProvider.getDecoder(Decoder.class);
        assertEquals(Decoder.Default.class, decodec.getClass());
    }

    @Test
    public void testNotProvidedCodec() {
        NotProvidedCodec codec = CodecProvider.getCoder(NotProvidedCodec.class);
        assertNull(codec);
        codec = CodecProvider.getEncoder(NotProvidedCodec.class);
        assertNull(codec);
        codec = CodecProvider.getDecoder(NotProvidedCodec.class);
        assertNull(codec);
    }

    public interface NotProvidedCodec extends Encoder, Decoder {

    }
}


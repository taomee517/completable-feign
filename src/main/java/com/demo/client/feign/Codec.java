package com.demo.client.feign;

import feign.codec.Decoder;
import feign.codec.Encoder;


public interface Codec extends Encoder, Decoder{
}
package com.demo.client.feign.completable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Future;

import feign.Contract;
import feign.MethodMetadata;
import feign.Util;

final class CompletableContract implements Contract {

    private final Contract delegate;

    CompletableContract(final Contract delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<MethodMetadata> parseAndValidateMetadata(final Class<?> targetType) {
        final List<MethodMetadata> metadataList = delegate.parseAndValidateMetadata(targetType);
        for (final MethodMetadata metadata : metadataList) {
            final Type type = metadata.returnType();
            if (type instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
                final Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                if (Future.class.isAssignableFrom(rawType)) {
                    metadata.returnType(Util.resolveLastTypeParameter(type, rawType));
                }
            }
        }
        return metadataList;
    }
}


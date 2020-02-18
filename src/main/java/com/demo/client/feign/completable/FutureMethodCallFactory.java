package com.demo.client.feign.completable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import feign.InvocationHandlerFactory;

@FunctionalInterface
public interface FutureMethodCallFactory {

    Future<?> create(final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch,
                     final Method method, final Object[] args, final Executor executor);
}

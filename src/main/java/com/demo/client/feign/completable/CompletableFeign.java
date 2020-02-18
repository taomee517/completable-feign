package com.demo.client.feign.completable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import com.demo.client.feign.CodecProvider;
import com.demo.client.feign.FeignProperties;
import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

public final class CompletableFeign {

    private CompletableFeign() {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends Feign.Builder {

        private Contract contract = new Contract.Default();
        private FutureMethodCallFactory futureFactory = (dispatch, method, args, executor) ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return dispatch.get(method).invoke(args);
                    } catch (final RuntimeException re) {
                        throw re;
                    } catch (final Throwable cause) {
                        throw new IllegalStateException("Impossible to reach here!", cause);
                    }
                }, executor);
        private Executor executor = ForkJoinPool.commonPool();
        private InvocationHandlerFactory invocationHandlerFactory = null;

        @Override
        public Builder contract(final Contract contract) {
            this.contract = contract;
            return this;
        }

        public Builder futureFactory(final FutureMethodCallFactory futureFactory) {
            this.futureFactory = futureFactory;
            return this;
        }

        public Builder executor(final Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public Builder invocationHandlerFactory(
                final InvocationHandlerFactory invocationHandlerFactory) {
            this.invocationHandlerFactory = invocationHandlerFactory;
            return this;
        }

        @Override
        public Builder logLevel(final Logger.Level logLevel) {
            super.logLevel(logLevel);
            return this;
        }

        @Override
        public Builder client(final Client client) {
            super.client(client);
            return this;
        }

        @Override
        public Builder retryer(final Retryer retryer) {
            super.retryer(retryer);
            return this;
        }

        @Override
        public Builder logger(final Logger logger) {
            super.logger(logger);
            return this;
        }

        @Override
        public Builder encoder(final Encoder encoder) {
            super.encoder(encoder);
            return this;
        }

        @Override
        public Builder decoder(final Decoder decoder) {
            super.decoder(decoder);
            return this;
        }

        public <C extends Encoder & Decoder> Builder coder(final C coder) {
            super.encoder(coder);
            super.decoder(coder);
            return this;
        }

        public Builder encoder(final Class<? extends Encoder> encoderClass) {
            super.encoder(CodecProvider.getEncoder(encoderClass));
            return this;
        }

        public Builder decoder(final Class<? extends Decoder> decoderClass) {
            super.decoder(CodecProvider.getDecoder(decoderClass));
            return this;
        }

        public <C extends Encoder & Decoder> Builder coder(final Class<C> coderClass) {
            CodecProvider.configureCoder(this, coderClass);
            return this;
        }

        @Override
        public Builder decode404() {
            super.decode404();
            return this;
        }

        @Override
        public Builder errorDecoder(final ErrorDecoder errorDecoder) {
            super.errorDecoder(errorDecoder);
            return this;
        }

        @Override
        public Builder options(final Request.Options options) {
            super.options(options);
            return this;
        }

        @Override
        public Builder requestInterceptor(final RequestInterceptor requestInterceptor) {
            super.requestInterceptor(requestInterceptor);
            return this;
        }

        @Override
        public Builder requestInterceptors(final Iterable<RequestInterceptor> requestInterceptors) {
            super.requestInterceptors(requestInterceptors);
            return this;
        }

        @Override
        public Feign build() {
            super.invocationHandlerFactory(invocationHandlerFactory == null ? (target, dispatch) ->
                    new CompletableInvocationHandler(target, dispatch, futureFactory, executor)
                    : invocationHandlerFactory);
            super.contract(new CompletableContract(contract));
            return super.build();
        }

        public <T> T target(final Class<T> apiType) {
            return target(new Target.HardCodedTarget<>(
                    apiType, FeignProperties.TARGET_URL.getProperty(apiType)));
        }
    }
}


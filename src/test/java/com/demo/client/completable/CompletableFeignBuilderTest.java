package com.demo.client.completable;

import com.demo.client.feign.Codec;
import com.demo.client.feign.FeignProperties;
import com.demo.client.feign.completable.CompletableFeign;
import com.demo.client.feign.completable.CompletableInvocationHandler;
import com.demo.client.test.CodecProviderTest;
import com.demo.client.test.TestCodec;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import feign.Client;
import feign.Contract;
import feign.Logger;
import feign.Request;
import feign.RequestLine;
import feign.Response;
import feign.Retryer;
import feign.Util;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class CompletableFeignBuilderTest {

    @Rule
    public final MockWebServer server = new MockWebServer();

    private String url;

    @Before
    public void beforeTest() {
        server.enqueue(new MockResponse().setBody("response data"));
        url = "http://localhost:" + server.getPort();
    }

    @Test
    public void testDefaults() throws Exception {
        final TestInterface api = CompletableFeign.builder().target(TestInterface.class, url);
        final Response response = api.post("request data");
        assertEquals("response data", Util.toString(response.body().asReader()));
        assertEquals("request data", server.takeRequest().getBody().readString(UTF_8));
    }

    @Test
    public void testSystemProperties() throws Exception {
        assertEquals("absent", FeignProperties.TARGET_URL
                .getProperty(TestInterface.class, "absent"));
        FeignProperties.TARGET_URL.setProperty(TestInterface.class, url);
        final TestInterface api = CompletableFeign.builder().target(TestInterface.class);
        final Response response = api.post("request data");
        assertEquals("response data", Util.toString(response.body().asReader()));
        assertEquals("request data", server.takeRequest().getBody().readString(UTF_8));
    }

    @Test
    public void testDelegates() throws Exception {
        final TestInterface api = CompletableFeign.builder()
                .logLevel(Logger.Level.BASIC)
                .logger(new Logger.ErrorLogger())
                .client(new Client.Default(null, null))
                .retryer(new Retryer.Default())
                .encoder(new Encoder.Default())
                .decoder(new Decoder.Default())
                .decode404()
                .errorDecoder(new ErrorDecoder.Default())
                .options(new Request.Options())
                .requestInterceptor(new BasicAuthRequestInterceptor("user", "pass"))
                .requestInterceptors(Collections.emptyList())
                .target(TestInterface.class, url);
        final Response response = api.post("request data");
        assertEquals("response data", Util.toString(response.body().asReader()));
        assertEquals("request data", server.takeRequest().getBody().readString(UTF_8));
    }

    @Test
    public void testProvideContract() throws Exception {
        final TestInterface api = CompletableFeign.builder().contract(new Contract.Default())
                .target(TestInterface.class, url);
        final Response response = api.post("request data");
        assertEquals("response data", Util.toString(response.body().asReader()));
        assertEquals("request data", server.takeRequest().getBody().readString(UTF_8));
        server.enqueue(new MockResponse().setBody("response data"));
        final CompletableFuture<Response> responseFuture = api.get();
        assertEquals("response data", Util.toString(responseFuture.join().body().asReader()));
    }

    @Test
    public void testProvideExecutor() throws Exception {
        final ExecutorService exec = Executors.newSingleThreadExecutor();
        final TestInterface api = CompletableFeign.builder()
                .executor(exec).target(TestInterface.class, url);
        final CompletableFuture<Response> response = api.get();
        assertEquals("response data", Util.toString(response.join().body().asReader()));
        exec.shutdown();
    }

    @Test
    public void testProvideCoder() throws Exception {
        final TestInterface api = CompletableFeign.builder()
                .coder(new TestCodec()).target(TestInterface.class, url);
        final CompletableFuture<Response> response = api.get();
        assertEquals("response data", Util.toString(response.join().body().asReader()));
    }

    @Test
    public void testLoadCoder() throws Exception {
        final TestInterface api = CompletableFeign.builder()
                .coder(Codec.class).target(TestInterface.class, url);
        final CompletableFuture<Response> response = api.get();
        assertEquals("response data", Util.toString(response.join().body().asReader()));
    }

    @Test
    public void testLoadNotProvidedCoder() throws Exception {
        final TestInterface api = CompletableFeign.builder()
                .coder(CodecProviderTest.NotProvidedCodec.class)
                .target(TestInterface.class, url);
        final CompletableFuture<Response> response = api.get();
        assertEquals("response data", Util.toString(response.join().body().asReader()));
    }

    @Test
    public void testLoadDeEncoder() throws Exception {
        final TestInterface api = CompletableFeign.builder()
                .decoder(Decoder.class)
                .encoder(Encoder.class)
                .target(TestInterface.class, url);
        final CompletableFuture<Response> response = api.get();
        assertEquals("response data", Util.toString(response.join().body().asReader()));
    }

    @Test
    public void testProvideFutureFactory() throws Exception {
        final TestInterface api = CompletableFeign.builder()
                .futureFactory((dispatch, method, args, executor) -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return dispatch.get(method).invoke(args);
                    } catch (final RuntimeException re) {
                        throw re;
                    } catch (final Throwable throwable) {
                        throw new CompletionException(throwable);
                    }
                }, executor)).target(TestInterface.class, url);
        final Response response = api.post("request data");
        assertEquals("response data", Util.toString(response.body().asReader()));
        assertEquals("request data", server.takeRequest().getBody().readString(UTF_8));
    }

    @Test
    public void testProvideInvocationHandlerFactory() throws Exception {
        final TestInterface api = CompletableFeign.builder()
                .invocationHandlerFactory((target, dispatch) ->
                        new CompletableInvocationHandler(target, dispatch,
                                (dispatchM, method, args, executor) -> CompletableFuture.supplyAsync(() -> {
                                    try {
                                        return dispatchM.get(method).invoke(args);
                                    } catch (final RuntimeException re) {
                                        throw re;
                                    } catch (final Throwable throwable) {
                                        throw new CompletionException(throwable);
                                    }
                                }, executor), ForkJoinPool.commonPool()))
                .target(TestInterface.class, url);
        final CompletableFuture<Response> response = api.get();
        assertEquals("response data", Util.toString(response.join().body().asReader()));
    }

    interface TestInterface {

        @RequestLine("GET")
        CompletableFuture<Response> get();

        @RequestLine("POST /")
        Response post(final String data);
    }
}


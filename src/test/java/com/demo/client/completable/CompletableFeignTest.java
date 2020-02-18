package com.demo.client.completable;

import com.demo.client.feign.completable.CompletableFeign;
import com.demo.client.feign.completable.CompletableInvocationHandler;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import feign.Feign;
import feign.FeignException;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.RetryableException;
import feign.Target;
import feign.gson.GsonDecoder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class CompletableFeignTest {

    @Rule
    public final MockWebServer server = new MockWebServer();

    @Test
    public void completableStringFuture() {
        server.enqueue(new MockResponse().setBody("\"foo\""));
        final CompletableFuture<String> future = TestInterface.create(server).command();
        assertNotNull(future);
        assertEquals("foo", future.join());
    }

    @Test
    public void stringFuture() throws ExecutionException, InterruptedException {
        server.enqueue(new MockResponse().setBody("\"foo\""));
        final Future<String> future = TestInterface.create(server).commandFuture();
        assertNotNull(future);
        assertEquals("foo", future.get());
    }

    @Test
    public void handleEx() {
        final int code = 500;
        server.enqueue(new MockResponse().setResponseCode(code));
        final CompletableFuture<String> future = TestInterface.create(server).command();
        final String result = future.handle((response, ex) -> ex == null ? response : "failed").join();
        assertEquals("failed", result);
    }

    @Test
    public void completableIntegerFuture() {
        server.enqueue(new MockResponse().setBody("1"));
        final CompletableFuture<Integer> future = TestInterface.create(server).intCommand();
        assertEquals(Integer.valueOf(1), future.join());
    }

    @Test
    public void completableStringListFuture() {
        server.enqueue(new MockResponse().setBody("[\"foo\",\"bar\"]"));
        final CompletableFuture<List<String>> future = TestInterface.create(server).listCommand();
        assertEquals(Arrays.asList("foo", "bar"), future.join());
    }

    @Test
    public void plainString() {
        server.enqueue(new MockResponse().setBody("\"foo\""));
        final String string = TestInterface.create(server).get();
        assertEquals("foo", string);
    }

    @Test
    public void plainList() {
        server.enqueue(new MockResponse().setBody("[\"foo\",\"bar\"]"));
        final List<String> list = TestInterface.create(server).getList();
        assertEquals(Arrays.asList("foo", "bar"), list);
    }

    interface GitHub {

        @RequestLine("GET /repos/{owner}/{repo}/contributors")
        CompletableFuture<List<String>> contributors(@Param("owner") String owner,
                                                     @Param("repo") String repo);
    }

    @Test
    public void handleResponse() {
        server.enqueue(new MockResponse().setResponseCode(500));
        final GitHub api = CompletableFeign.builder()
                .target(GitHub.class, "http://localhost:" + server.getPort());
        final List<String> contributors = api
                .contributors("OpenFeign", "feign")
                .handle((response, ex) -> ex == null
                        ? response : Collections.singletonList("adriancole")).join();
        assertEquals(Collections.singletonList("adriancole"), contributors);
    }

    @Test
    public void testRuntimeExceptions() {
        final int code = 500;
        server.enqueue(new MockResponse().setResponseCode(code));
        final GitHub api = CompletableFeign.builder()
                .target(GitHub.class, "http://localhost:" + server.getPort());
        final Throwable throwable = api.contributors("OpenFeign", "feign")
                .handle((resp, ex) -> ex).join();
        assertEquals(CompletionException.class, throwable.getClass());
        final Throwable cause = throwable.getCause();
        assertEquals(FeignException.class, cause.getClass());
        assertEquals(code, ((FeignException) cause).status());
    }

    @Test(expected = Throwable.class)
    public void testDefaultThrowable() throws Throwable {
        TestInterface.create(server).throwThrowable();
    }

    @Test
    public void testThrowable() throws Throwable {
        final TestInterface api = CompletableFeign.builder()
                .target(TestInterface.class, "http://localhost:1111");
        final CompletableFuture<String> future = api.command();
        final Throwable throwable = future.handle((resp, ex) -> ex).join();
        assertEquals(CompletionException.class, throwable.getClass());
        final Throwable cause = throwable.getCause();
        assertEquals(RetryableException.class, cause.getClass());
    }

    @Test
    public void testDefaultMethod() {
        server.enqueue(new MockResponse().setBody("\"default\""));
        final CompletableFuture<String> future = TestInterface.create(server).defaultCommand();
        assertEquals("default", future.join());
    }

    @Test
    public void equalsHashCodeAndToStringWork() {
        final Target<TestInterface> t1 =
                new Target.HardCodedTarget<>(TestInterface.class, "http://localhost:8080");
        final Target<TestInterface> t2 =
                new Target.HardCodedTarget<>(TestInterface.class, "http://localhost:8888");
        final Target<OtherTestInterface> t3 =
                new Target.HardCodedTarget<>(OtherTestInterface.class, "http://localhost:8080");
        final TestInterface i1 = CompletableFeign.builder().target(t1);
        final TestInterface i2 = CompletableFeign.builder().target(t1);
        final TestInterface i3 = CompletableFeign.builder().target(t2);
        final OtherTestInterface i4 = CompletableFeign.builder().target(t3);
        final OtherTestInterface i5 = Feign.builder().target(t3);

        assertEquals(i1, i2);
        assertNotEquals(i1, i3);
        assertNotEquals(i1, i4);
        assertNotEquals(i1, i5);
        assertFalse(i1.equals(""));
        assertFalse(i1.equals(null));

        final int i1HashCode = i1.hashCode();
        assertEquals(i1HashCode, i2.hashCode());
        assertNotEquals(i1HashCode, i3.hashCode());
        assertNotEquals(i1HashCode, i4.hashCode());

        final String i1String = i1.toString();
        assertEquals(i1String, i2.toString());
        assertNotEquals(i1String, i3.toString());
        assertNotEquals(i1String, i4.toString());

        assertNotEquals(i1, t1);
        assertFalse(t1.equals(""));
        assertFalse(t1.equals(null));
        assertEquals(i1HashCode, t1.hashCode());
        assertEquals(i1String, t1.toString());

        final CompletableInvocationHandler direct =
                new CompletableInvocationHandler(t1, null, null, null);
        assertEquals(direct, direct);
        assertEquals(direct, new CompletableInvocationHandler(t1, null, null, null));
        assertNotEquals(direct, t1);
        assertNotEquals(direct, t2);
        assertNotEquals(direct, t3);
        assertFalse(direct.equals(""));
        assertFalse(direct.equals(null));
    }

    interface OtherTestInterface {

        @RequestLine("GET /")
        @Headers("Accept: application/json")
        CompletableFuture<List<String>> listCommand();
    }

    interface TestInterface {

        static TestInterface create(final MockWebServer server) {
            return CompletableFeign.builder()
                    .decoder(new GsonDecoder())
                    .target(TestInterface.class, "http://localhost:" + server.getPort());
        }

        @RequestLine("GET /")
        @Headers("Accept: application/json")
        CompletableFuture<List<String>> listCommand();

        @RequestLine("GET /")
        @Headers("Accept: application/json")
        CompletableFuture<String> command();

        @RequestLine("GET /")
        @Headers("Accept: application/json")
        Future<String> commandFuture();

        @RequestLine("GET /")
        @Headers("Accept: application/json")
        CompletableFuture<Integer> intCommand();

        @RequestLine("GET /")
        @Headers("Accept: application/json")
        String get();

        @RequestLine("GET /")
        @Headers("Accept: application/json")
        List<String> getList();

        default CompletableFuture<Void> throwThrowable() throws Throwable {
            throw new Throwable();
        }

        default CompletableFuture<String> defaultCommand() {
            return command();
        }
    }
}


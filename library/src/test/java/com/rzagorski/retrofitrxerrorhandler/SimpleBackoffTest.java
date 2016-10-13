package com.rzagorski.retrofitrxerrorhandler;

import com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Simple;
import com.rzagorski.retrofitrxerrorhandler.model.GitHub;
import com.rzagorski.retrofitrxerrorhandler.utils.MockWebServerUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.SocketTimeoutException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.observers.TestSubscriber;

import static com.rzagorski.retrofitrxerrorhandler.utils.MockWebServerUtils.createRetrofitInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Robert Zagórski on 2016-10-03.
 */

@RunWith(MockitoJUnitRunner.class)
public class SimpleBackoffTest {

    MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer = null;
    }

    @Test
    public void testBackoffError1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Simple.init()
                        .addThrowable(SocketTimeoutException.class)
                        .setMaxRetries(3).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        long startTime = System.currentTimeMillis();
        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions
        assertTrue((endTime - startTime) >= 4 * 10 * MockWebServerUtils.ONE_SEC);
    }


    @Test
    public void testBackoffCompletes1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setResponseCode(500);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Simple.init()
                        .addHttpCode(500)
                        .setMaxRetries(300).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(HttpException.class);
    }

    @Test
    public void testBackoffCompletes2() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setResponseCode(500);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Simple.init()
                        .addHttpCode(500)
                        .setMaxRetries(300).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertEquals(testSubscriber.getOnErrorEvents().size(), 1);
    }
}

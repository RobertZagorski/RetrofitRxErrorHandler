package com.rzagorski.retrofitrxerrorhandler;

import com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Exponential;
import com.rzagorski.retrofitrxerrorhandler.model.GitHub;
import com.rzagorski.retrofitrxerrorhandler.utils.MockWebServerUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.mockwebserver.MockWebServer;
import rx.Observable;
import rx.observers.TestSubscriber;

import static com.rzagorski.retrofitrxerrorhandler.utils.MockWebServerUtils.createRetrofitInstance;
import static org.junit.Assert.assertTrue;

/**
 * Created by Robert Zagórski on 2016-09-29.
 */

@RunWith(MockitoJUnitRunner.class)
public class RxErrorHandingFactoryTest {

    MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.enqueue(MockWebServerUtils.getSuccessfulResponse());
        mockWebServer.start();
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer = null;
    }

    @Test
    public void testSimpleError() throws Exception {
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(UnknownHostException.class)
                        .addThrowable(SocketTimeoutException.class)
                        .setMaxRetries(3).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void testSimpleSuccessfulResponse() throws Exception {
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(UnknownHostException.class)
                        .addThrowable(SocketTimeoutException.class)
                        .setMaxRetries(3).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        assertTrue(testSubscriber.getOnNextEvents().size() == 1);
    }

    @Test
    public void testSimpleSuccessfulResponse2() throws Exception {
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(UnknownHostException.class)
                        .addThrowable(SocketTimeoutException.class)
                        .setMaxRetries(3).build())
                .build();

        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.assertCompleted();
    }
}

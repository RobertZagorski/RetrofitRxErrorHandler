/*
 * Copyright (C) 2016 Robert Zagórski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.observers.TestSubscriber;

import static com.rzagorski.retrofitrxerrorhandler.utils.MockWebServerUtils.createRetrofitInstance;
import static org.junit.Assert.assertTrue;

/**
 * Created by Robert Zagórski on 2016-10-03.
 */

@RunWith(MockitoJUnitRunner.class)
public class ExponentialBackoffTest {

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
    public void testBackoffOneTime1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() == 1) {
                    return new MockResponse().setResponseCode(500);
                } else if (mockWebServer.getRequestCount() == 2) {
                    return MockWebServerUtils.getSuccessfulResponse();
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(HttpException.class)
                        .setMaxRetries(3).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertTrue(testSubscriber.getOnNextEvents().size() == 1);
    }

    @Test
    public void testBackoffOneTime2() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() == 1) {
                    return new MockResponse().setResponseCode(500);
                } else if (mockWebServer.getRequestCount() == 2) {
                    return MockWebServerUtils.getSuccessfulResponse();
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(HttpException.class)
                        .setBase(1)
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
        System.out.println(endTime - startTime);
        assertTrue((endTime - startTime) >= MockWebServerUtils.ONE_SEC && (endTime - startTime) <= MockWebServerUtils.ONE_SEC * 2);
    }

    @Test
    public void testBackoffTwoTimes() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() < 3) {
                    return new MockResponse().setResponseCode(500);
                } else if (mockWebServer.getRequestCount() == 3) {
                    return MockWebServerUtils.getSuccessfulResponse();
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(HttpException.class)
                        .setMaxRetries(3).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertTrue(testSubscriber.getOnNextEvents().size() == 1);
    }

    @Test
    public void testBackoffCompletes() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return MockWebServerUtils.getSuccessfulResponse();
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(HttpException.class)
                        .setMaxRetries(3).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testBackoffError1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setResponseCode(500);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(HttpException.class)
                        .setMaxRetries(3).build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertTrue(testSubscriber.getOnErrorEvents().size() == 1);
    }

    @Test
    public void testBackoffError2() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setResponseCode(500);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(HttpException.class)
                        .setMaxRetries(3).build())
                .setLoggingEnabled(true)
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
        assertTrue((endTime - startTime) >= 13 * MockWebServerUtils.ONE_SEC);
    }

    @Test
    public void testBackoffError3() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .exclusive()
                        .addThrowable(SocketTimeoutException.class)
                        .setBase(1)
                        .setMaxRetries(1).build())
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
        //one SocketTimeoutException
        assertTrue((endTime - startTime) <= (10 + 1) * MockWebServerUtils.ONE_SEC);
    }
}

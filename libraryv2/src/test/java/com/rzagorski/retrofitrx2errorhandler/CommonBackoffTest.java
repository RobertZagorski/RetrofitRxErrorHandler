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
package com.rzagorski.retrofitrx2errorhandler;

import com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Exponential;
import com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Simple;
import com.rzagorski.retrofitrx2errorhandler.model.GitHub;
import com.rzagorski.retrofitrx2errorhandler.utils.MockWebServerUtils;
import com.rzagorski.retrofitrx2errorhandler.utils.TestObservable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.SocketTimeoutException;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import retrofit2.HttpException;

import static com.rzagorski.retrofitrx2errorhandler.utils.MockWebServerUtils.createRetrofitInstance;
import static org.junit.Assert.assertTrue;

/**
 * Created by Robert Zagórski on 2016-10-03.
 */

@RunWith(MockitoJUnitRunner.class)
public class CommonBackoffTest {

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

    /**
     * This test show how to create CallAdapter.Factory without adding a reaction for errors.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testCompletes1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return MockWebServerUtils.getSuccessfulResponse();
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }


    /**
     * Test shows, that when the server responds with different error
     * {@link retrofit2.HttpException} than
     * {@link com.rzagorski.retrofitrx2errorhandler.backoff.BackoffStrategy backoff strategies} react to
     * ({@link java.net.SocketTimeoutException}) the execution ends immediately.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testCompletes2() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                switch (mockWebServer.getRequestCount()) {
                    case 1:
                        return new MockResponse().setResponseCode(500);
                    case 2:
                        return new MockResponse().setResponseCode(404);
                    case 3:
                        return MockWebServerUtils.getSuccessfulResponse();
                    default:
                        return new MockResponse().setResponseCode(500);
                }
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(SocketTimeoutException.class)
                        .setBase(2)
                        .setMaxRetries(3).build())
                .addBackoffStrategy(Simple.init()
                        .addThrowable(SocketTimeoutException.class)
                        .setMaxRetries(3).build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        long startTime = System.currentTimeMillis();
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions and 3 exponential backoff's
        assertTrue((endTime - startTime) <= (1) * MockWebServerUtils.ONE_SEC);
    }

    /**
     * Test shows the usage of two
     * {@link com.rzagorski.retrofitrx2errorhandler.backoff.BackoffStrategy backoff strategies}.
     * Shows that the delay is greater than 3 exponential backoff strategy invocation, that reacts
     * to {@link retrofit2.HttpException} and 3 simple strategies, that reacts to {@link java.net.SocketTimeoutException}
     * <br>
     * Shows that every backoff strategy is invoked as many times as in
     * {@link com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Exponential.Builder#setMaxRetries(int)}
     * or in {@link com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Simple.Builder#setMaxRetries(int)}
     * independently.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackoffError1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() % 2 == 0) {
                    return new MockResponse().setResponseCode(500);
                } else {
                    return new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE);
                }
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(HttpException.class)
                        .setBase(2)
                        .setMaxRetries(3).build())
                .addBackoffStrategy(Simple.init()
                        .addThrowable(SocketTimeoutException.class)
                        .setMaxRetries(3).build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        long startTime = System.currentTimeMillis();
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions and 3 exponential backoff's
        assertTrue((endTime - startTime) >= (1 + 2 + 4 + 8) * MockWebServerUtils.ONE_SEC + 4 * 10 * MockWebServerUtils.ONE_SEC);
    }

    /**
     * Test shows the usage of two
     * {@link com.rzagorski.retrofitrx2errorhandler.backoff.BackoffStrategy backoff strategies}.
     * Shows that the delay is greater than 3 exponential backoff strategy invocation, that reacts
     * to 500 server error and 3 simple strategies, that reacts to {@link java.net.SocketTimeoutException}
     * <br>
     * Shows that every backoff strategy is invoked as many times as in
     * {@link com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Exponential.Builder#setMaxRetries(int)}
     * or in {@link com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Simple.Builder#setMaxRetries(int)}
     * independently.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackoffError2() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() % 2 == 0) {
                    return new MockResponse().setResponseCode(500);
                } else {
                    return new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE);
                }
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addHttpCode(500)
                        .setBase(2)
                        .setMaxRetries(3).build())
                .addBackoffStrategy(Simple.init()
                        .addThrowable(SocketTimeoutException.class)
                        .setMaxRetries(3).build())
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        long startTime = System.currentTimeMillis();
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions and 3 exponential backoff's
        assertTrue((endTime - startTime) >= (1 + 2 + 4 + 8) * MockWebServerUtils.ONE_SEC + 4 * 10 * MockWebServerUtils.ONE_SEC);
    }

    /**
     * Test shows the usage of one
     * {@link com.rzagorski.retrofitrx2errorhandler.backoff.BackoffStrategy backoff strategy}.
     * Shows that there is minimal delay, if the response from server is different than strategy
     * reacts to.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackoffError3() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setResponseCode(500);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(SocketTimeoutException.class)
                        .setBase(2)
                        .setMaxRetries(3).build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        long startTime = System.currentTimeMillis();
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        assertTrue((endTime - startTime) <= 1 * 1000L);
    }

    /**
     * Test shows the usage of two
     * {@link com.rzagorski.retrofitrx2errorhandler.backoff.BackoffStrategy backoff strategies}.
     * Shows that there is minimal delay, if the response from server is different than strategies
     * react to.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackoffError4() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setResponseCode(500);
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(SocketTimeoutException.class)
                        .setBase(2)
                        .setMaxRetries(3).build())
                .addBackoffStrategy(Exponential.init()
                        .addHttpCode(404)
                        .build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));

        long startTime = System.currentTimeMillis();
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        assertTrue((endTime - startTime) <= MockWebServerUtils.ONE_SEC);
    }

    /**
     * Test shows the usage of backup `Observable`. Checks if successful response is passed to
     * `Subscriber` even if backup `Observable` is invoked.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackupObservable1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() == 1) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > 1) {
                    return MockWebServerUtils.getSuccessfulResponse();
                }
                return new MockResponse().setResponseCode(500);
            }
        });

        Observable<Boolean> backupObservable = Observable.just(Boolean.TRUE);

        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addHttpCode(404)
                        .addObservable(backupObservable)
                        .setBase(2)
                        .setMaxRetries(3)
                        .build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    /**
     * Test shows the usage of backup `Observable`. Checks if it is invoked at least one time.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackupObservable2() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() <= 3) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > 3) {
                    return MockWebServerUtils.getSuccessfulResponse();
                }
                return new MockResponse().setResponseCode(500);
            }
        });

        Observable<Boolean> backupObservable = Observable.just(Boolean.TRUE);
        TestObservable testBackupObservable = new TestObservable();
        backupObservable = testBackupObservable.wrap(backupObservable);

        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addHttpCode(404)
                        .addObservable(backupObservable)
                        .setBase(2)
                        .setMaxRetries(3)
                        .build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        testObserver.awaitTerminalEvent();
        testBackupObservable.assertStarted();
    }

    /**
     * Test shows the usage of backup `Observable`. Checks if is in invoked the same amount of times as
     * the error response comes from server
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackupObservable3() throws Exception {
        final int REQUEST_COUNT = 3;
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() <= REQUEST_COUNT) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > REQUEST_COUNT) {
                    return MockWebServerUtils.getSuccessfulResponse();
                }
                return new MockResponse().setResponseCode(500);
            }
        });

        Observable<Boolean> backupObservable = Observable.just(Boolean.TRUE);
        TestObservable testBackupObservable = new TestObservable();
        backupObservable = testBackupObservable.wrap(backupObservable);

        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addHttpCode(404)
                        .addObservable(backupObservable)
                        .setBase(2)
                        .setMaxRetries(3)
                        .build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        assert testBackupObservable.getOnNextEvents().size() == REQUEST_COUNT;
    }

    /**
     * Test shows the usage of backup `Observable`. Checks if it completes at least one time.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackupObservable4() throws Exception {
        final int REQUEST_COUNT = 3;
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() <= REQUEST_COUNT) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > REQUEST_COUNT) {
                    return MockWebServerUtils.getSuccessfulResponse();
                }
                return new MockResponse().setResponseCode(500);
            }
        });

        Observable<Boolean> backupObservable = Observable.just(Boolean.TRUE);
        TestObservable testBackupObservable = new TestObservable();
        backupObservable = testBackupObservable.wrap(backupObservable);

        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addHttpCode(404)
                        .addObservable(backupObservable)
                        .setBase(2)
                        .setMaxRetries(3)
                        .build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        testBackupObservable.assertCompleted();
    }

    /**
     * Test shows the usage of backup `Observable`. Checks if successful response is passed to
     * `Subscriber` even if backup `Observable` is invoked and it does not emits any elements.
     * <br>
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void testBackupObservable5() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() == 1) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > 1) {
                    return MockWebServerUtils.getSuccessfulResponse();
                }
                return new MockResponse().setResponseCode(500);
            }
        });

        Observable<Boolean> backupObservable = Observable.empty();

        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addHttpCode(404)
                        .addObservable(backupObservable)
                        .setBase(2)
                        .setMaxRetries(3)
                        .build())
                .setLoggingEnabled(true)
                .build();
        GitHub github = createRetrofitInstance(mockWebServer.url("/").toString(),
                new RxErrorHandingFactory(rxCallAdapter));
        Observable observable = github.repos("square");
        TestObserver testObserver = observable.test();
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }
}

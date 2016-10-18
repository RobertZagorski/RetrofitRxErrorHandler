package com.rzagorski.retrofitrxerrorhandler;

import com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Exponential;
import com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Simple;
import com.rzagorski.retrofitrxerrorhandler.model.GitHub;
import com.rzagorski.retrofitrxerrorhandler.utils.MockWebServerUtils;
import com.rzagorski.retrofitrxerrorhandler.utils.TestObservable;

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

    @Test
    public void testCompletes1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                try {
                    return MockWebServerUtils.getSuccessfulResponse();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new MockResponse();
            }
        });
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .setLoggingEnabled(true)
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
                        try {
                            return MockWebServerUtils.getSuccessfulResponse();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return new MockResponse();
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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions and 3 exponential backoff's
        assertTrue((endTime - startTime) <= (1) * MockWebServerUtils.ONE_SEC);
    }

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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions and 3 exponential backoff's
        assertTrue((endTime - startTime) >= (1 + 2 + 4 + 8) * MockWebServerUtils.ONE_SEC + 4 * 10 * MockWebServerUtils.ONE_SEC);
    }

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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions and 3 exponential backoff's
        assertTrue((endTime - startTime) >= (1 + 2 + 4 + 8) * MockWebServerUtils.ONE_SEC + 4 * 10 * MockWebServerUtils.ONE_SEC);
    }

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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions and 3 exponential backoff's
        assertTrue((endTime - startTime) <= 1 * 1000L);
    }

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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime));
        //four SocketTimeoutExceptions and 3 exponential backoff's
        assertTrue((endTime - startTime) <= MockWebServerUtils.ONE_SEC);
    }

    @Test
    public void testBackupObservable1() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() == 1) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > 1) {
                    try {
                        return MockWebServerUtils.getSuccessfulResponse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testBackupObservable2() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() <= 3) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > 3) {
                    try {
                        return MockWebServerUtils.getSuccessfulResponse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testBackupObservable.assertStarted();
    }

    @Test
    public void testBackupObservable3() throws Exception {
        final int REQUEST_COUNT = 3;
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() <= REQUEST_COUNT) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > REQUEST_COUNT) {
                    try {
                        return MockWebServerUtils.getSuccessfulResponse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assert testBackupObservable.getOnNextEvents().size() == REQUEST_COUNT;
    }

    @Test
    public void testBackupObservable4() throws Exception {
        final int REQUEST_COUNT = 3;
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (mockWebServer.getRequestCount() <= REQUEST_COUNT) {
                    return new MockResponse().setResponseCode(404);
                } else if (mockWebServer.getRequestCount() > REQUEST_COUNT) {
                    try {
                        return MockWebServerUtils.getSuccessfulResponse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testBackupObservable.assertCompleted();
    }
}

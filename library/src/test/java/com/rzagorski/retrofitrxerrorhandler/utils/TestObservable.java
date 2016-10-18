package com.rzagorski.retrofitrxerrorhandler.utils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Robert Zagórski on 2016-10-18.
 */

public class TestObservable<T> {
    Observable<T> testedObservable;

    private long subsciptionTime;
    private long completionTime;

    private volatile Thread executionThread;

    List<T> onNextEvents;
    List<Throwable> onErrorEvents;

    private int completions;

    public TestObservable() {
        onNextEvents = new ArrayList<>();
        onErrorEvents = new ArrayList<>();

    }

    public void assertStarted() {
        assert onNextEvents.size() > 0;
    }

    public List<T> getOnNextEvents() {
        return onNextEvents;
    }

    public List<Throwable> getOnErrorEvents() {
        return onErrorEvents;
    }

    public void assertCompleted() {
        assert completions > 0;
    }

    public void assertNoErrors() {
        assert onErrorEvents.size() > 0;
    }

    public Observable<T> wrap(Observable<T> observable) {
        if (observable == null) {
            throw new NullPointerException();
        }
        return observable
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        subsciptionTime = System.currentTimeMillis();
                    }
                })
                .doOnNext(new Action1<T>() {
                    @Override
                    public void call(T t) {
                        onNextEvents.add(t);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onErrorEvents.add(throwable);
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        completions++;
                        completionTime = System.currentTimeMillis();
                    }
                })
                .map(new Func1<T, T>() {
                    @Override
                    public T call(T t) {
                        executionThread = Thread.currentThread();
                        return t;
                    }
                });
    }
}

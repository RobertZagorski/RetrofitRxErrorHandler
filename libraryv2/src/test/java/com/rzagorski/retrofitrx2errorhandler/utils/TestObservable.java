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
package com.rzagorski.retrofitrx2errorhandler.utils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * A helper class to detect events done on {@link io.reactivex.Observable}
 * <br>
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
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        subsciptionTime = System.currentTimeMillis();
                    }
                })
                .doOnNext(new Consumer<T>() {
                    @Override
                    public void accept(T t) {
                        onNextEvents.add(t);
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        onErrorEvents.add(throwable);
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        completions++;
                        completionTime = System.currentTimeMillis();
                    }
                })
                .map(new Function<T, T>() {
                    @Override
                    public T apply(T t) {
                        executionThread = Thread.currentThread();
                        return t;
                    }
                });
    }
}

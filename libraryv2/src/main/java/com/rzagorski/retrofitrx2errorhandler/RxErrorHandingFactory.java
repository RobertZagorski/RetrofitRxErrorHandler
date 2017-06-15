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

import com.rzagorski.retrofitrx2errorhandler.backoff.BackoffStrategy;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;

/**
 * Main class for reacting to errors that were thrown during making a
 * <a href="https://github.com/square/retrofit/blob/master/retrofit/src/main/java/retrofit2/Call.java">Call</a>.
 * <br>
 * Created by Robert Zagórski on 2016-09-28.
 */
public class RxErrorHandingFactory extends BaseRxCallAdapterFactory {
    RxCallAdapter info;

    public RxErrorHandingFactory(RxCallAdapter callAdapter) {
        super();
        this.info = callAdapter;
        for (BackoffStrategy strategy : info.getBackoffStrategies()) {
            strategy.setLoggingEnabled(info.isLoggingEnabled());
        }
    }

    protected <T> ObservableTransformer<T, T> transformRequest() {
        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> request) {
                return (Observable<T>) request
                        .compose(returnProperBackStrategy());
            }
        };
    }

    private <T> ObservableTransformer<T, T> returnProperBackStrategy() {
        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> observable) {
                return observable.retryWhen(new Function<Observable<? extends Throwable>, Observable<?>>() {
                    @Override
                    public Observable<?> apply(final Observable<? extends Throwable> error) {
                        return error
                                .flatMap(new IsRepeatableError(info.getBackoffStrategies()))
                                .compose(new PassErrorToBackoffStrategies<T>(info.getBackoffStrategies()));
                    }
                });
            }
        };
    }

    private class IsRepeatableError implements Function<Throwable, Observable<? extends Throwable>> {
        List<BackoffStrategy> backoffStrategyList;

        public IsRepeatableError(List<BackoffStrategy> backoffStrategyList) {
            this.backoffStrategyList = backoffStrategyList;
        }

        @Override
        public Observable<? extends Throwable> apply(Throwable throwable) throws Exception {
            boolean isRepeatable = false;
            for (BackoffStrategy strategy : backoffStrategyList) {
                if (strategy.isApplicable(throwable)) {
                    isRepeatable = true;
                }
            }
            if (!isRepeatable) {
                return Observable.error(throwable);
            }
            return Observable.just(throwable);
        }
    }

    private class PassErrorToBackoffStrategies<T> implements ObservableTransformer<Throwable, T> {
        List<BackoffStrategy> backoffStrategyList;

        public PassErrorToBackoffStrategies(List<BackoffStrategy> backoffStrategyList) {
            this.backoffStrategyList = backoffStrategyList;
        }

        @Override
        public Observable<T> apply(final Observable<Throwable> error) {
            return Observable.fromIterable(backoffStrategyList)
                    .flatMap(new Function<BackoffStrategy, Observable<T>>() {
                        @Override
                        public Observable<T> apply(BackoffStrategy backoffStrategy) throws Exception {
                            return (Observable<T>) backoffStrategy.apply(error);
                        }
                    });
        }
    }
}
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
package com.rzagorski.retrofitrx2errorhandler.backoff;

import com.rzagorski.retrofitrx2errorhandler.backoff.retryBehavior.ExclusiveRetryIfBehaviour;
import com.rzagorski.retrofitrx2errorhandler.backoff.retryBehavior.InclusiveRetryIfBehaviour;
import com.rzagorski.retrofitrx2errorhandler.backoff.strategies.AddReaction;
import com.rzagorski.retrofitrx2errorhandler.utils.ObservableUtils;
import com.rzagorski.retrofitrx2errorhandler.utils.Pair;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * Created by Robert Zagórski on 2016-09-28.
 */
public abstract class BaseBackoffStrategy implements BackoffStrategy {
    private boolean isExclusive;
    private List<Class<? extends Throwable>> throwableList;
    private Function<Throwable, Boolean> retryIfFunc;
    private BiConsumer<Throwable, Integer> doOnRetryAction;
    private boolean isLoggingEnabled;
    private Observable backupObservable;

    protected BaseBackoffStrategy(Builder builder) {
        this.isExclusive = builder.isExclusive;
        this.throwableList = builder.throwableList;
        this.retryIfFunc = builder.retryIfFunction;
        if (this.retryIfFunc == null) {
            if (isExclusive) {
                this.retryIfFunc = new ExclusiveRetryIfBehaviour(builder.throwableList, builder.httpCodeList);
            } else {
                this.retryIfFunc = new InclusiveRetryIfBehaviour(builder.throwableList, builder.httpCodeList);
            }
        }
        this.doOnRetryAction = builder.doOnRetryAction;
        if (this.doOnRetryAction == null) {
            this.doOnRetryAction = new DefaultDoOnRetryAction();
        }
        this.backupObservable = builder.observableToExecuteAfterError;
    }

    protected abstract Observable<Long> getWaitTime(int retry);

    protected abstract int getMaxRetries();

    public Function<Throwable, Boolean> getRetryIfFunction() {
        return retryIfFunc;
    }

    @Override
    public boolean isApplicable(Throwable throwable) throws Exception {
        return retryIfFunc.apply(throwable);
    }

    public BiConsumer<Throwable, Integer> doOnRetry(Throwable throwable, Integer retry) {
        return doOnRetryAction;
    }

    @Override
    public void setLoggingEnabled(boolean logging) {
        isLoggingEnabled = logging;
    }

    @Override
    public Observable getBackupObservable() {
        return backupObservable;
    }

    private void callAction(Throwable throwable, Integer retry) throws Exception {
        BiConsumer<Throwable, Integer> action = doOnRetry(throwable, retry);
        if (action == null) {
            return;
        }
        action.accept(throwable, retry);
    }

    @Override
    public Observable<?> apply(@NonNull Observable<? extends Throwable> attempts) throws Exception {
        return attempts
                .filter(new Predicate<Throwable>() {
                    @Override
                    public boolean test(@NonNull Throwable throwable) throws Exception {
                        if (isLoggingEnabled) {
                            System.out.println("Checking against: "
                                    + throwable.getClass().getSimpleName()
                                    + " for strategy: "
                                    + BaseBackoffStrategy.this.getClass().getSimpleName());
                        }
                        return getRetryIfFunction().apply(throwable);
                    }
                })
                .flatMap(new Function<Throwable, ObservableSource<? extends Throwable>>() {
                    @Override
                    public Observable<? extends Throwable> apply(final Throwable throwable) throws Exception {
                        if (backupObservable == null) {
                            return Observable.just(throwable);
                        }
                        if (isLoggingEnabled) {
                            System.out.println("Invoking backup observable");
                        }
                        return ((Observable<Object>) backupObservable)
                                .switchIfEmpty(Observable.just(throwable))
                                .flatMap(new Function<Object, Observable<? extends Throwable>>() {
                                    @Override
                                    public Observable<? extends Throwable> apply(Object o) {
                                        return Observable.just(throwable);
                                    }
                                });
                    }
                })
                .zipWith(Observable.range(1, getMaxRetries() + 1),
                        new ObservableUtils.RxPair<Throwable, Integer>())
                .doOnNext(new Consumer<Pair<Throwable, Integer>>() {
                    @Override
                    public void accept(Pair<Throwable, Integer> ti) throws Exception {
                        if (isLoggingEnabled) {
                            System.out.println("Found match: "
                                    + ti.first.getClass().getSimpleName()
                                    + " for strategy: "
                                    + BaseBackoffStrategy.this.getClass().getSimpleName()
                                    + " for "
                                    + ti.second + " retry");
                        }
                        callAction(ti.first, ti.second);
                    }
                })
                .flatMap(new Function<Pair<Throwable, Integer>, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Pair<Throwable, Integer> ti) {
                        if (ti.second <= getMaxRetries()) {
                            return getWaitTime(ti.second);
                        } else {
                            return Observable.error(ti.first);
                        }
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        if (isLoggingEnabled) {
                            throwable.printStackTrace();
                        }
                    }
                });
    }

    public static class Builder implements Optional, AddReaction<Builder> {
        private boolean isExclusive = false;
        private List<Class<? extends Throwable>> throwableList;
        private List<Integer> httpCodeList;
        private Function<Throwable, Boolean> retryIfFunction;
        private BiConsumer<Throwable, Integer> doOnRetryAction;
        private Observable<?> observableToExecuteAfterError;

        public Builder() {
            throwableList = new ArrayList<>();
            httpCodeList = new ArrayList<>();
        }

        @Override
        public Builder exclusive() {
            isExclusive = true;
            return this;
        }

        @Override
        public Builder addThrowable(Class<? extends Throwable> throwableForBackoff) {
            this.throwableList.add(throwableForBackoff);
            return this;
        }

        @Override
        public Builder setThrowable(List<Class<? extends Throwable>> throwableForBackoffList) {
            this.throwableList = throwableForBackoffList;
            return this;
        }

        @Override
        public Builder setHttpCodeList(List<Integer> code) {
            this.httpCodeList = code;
            return this;
        }

        @Override
        public Builder addHttpCode(int code) {
            this.httpCodeList.add(code);
            return this;
        }

        @Override
        public Builder addObservable(Observable<?> observable) {
            this.observableToExecuteAfterError = observable;
            return this;
        }

        @Override
        public Builder setRetryFunction(Function<Throwable, Boolean> retryIf) {
            this.retryIfFunction = retryIf;
            return this;
        }

        @Override
        public Builder setOnRetryAction(BiConsumer<Throwable, Integer> onRetryAction) {
            this.doOnRetryAction = onRetryAction;
            return this;
        }
    }

    public interface Optional {

        public Optional setOnRetryAction(BiConsumer<Throwable, Integer> onRetryAction);

        public Optional setRetryFunction(Function<Throwable, Boolean> retryIf);
    }
}
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
package com.rzagorski.retrofitrxerrorhandler.backoff;

import com.rzagorski.retrofitrxerrorhandler.backoff.retryBehavior.ExclusiveRetryIfBehaviour;
import com.rzagorski.retrofitrxerrorhandler.backoff.retryBehavior.InclusiveRetryIfBehaviour;
import com.rzagorski.retrofitrxerrorhandler.backoff.strategies.AddThrowable;
import com.rzagorski.retrofitrxerrorhandler.utils.ObservableUtils;
import com.rzagorski.retrofitrxerrorhandler.utils.Pair;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;

/**
 * Created by Robert Zagórski on 2016-09-28.
 */
public abstract class BaseBackoffStrategy implements BackoffStrategy {
    private boolean isExclusive;
    private List<Class<? extends Throwable>> throwableList;
    private Func1<Throwable, Boolean> retryIfFunc;
    private Action2<Throwable, Integer> doOnRetryAction;
    private boolean isLoggingEnabled;

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
    }

    protected abstract Observable<Long> getWaitTime(int retry);

    protected abstract int getMaxRetries();

    public Func1<Throwable, Boolean> getRetryIfFunction() {
        return retryIfFunc;
    }

    @Override
    public boolean isApplicable(Throwable throwable) {
        return retryIfFunc.call(throwable);
    }

    public Action2<Throwable, Integer> doOnRetry(Throwable throwable, Integer retry) {
        return doOnRetryAction;
    }

    @Override
    public void setLoggingEnabled(boolean logging) {
        isLoggingEnabled = logging;
    }

    private void callAction(Throwable throwable, Integer retry) {
        Action2<Throwable, Integer> action = doOnRetry(throwable, retry);
        if (action == null) {
            return;
        }
        action.call(throwable, retry);
    }

    @Override
    public Observable<?> call(Observable<? extends Throwable> attempts) {
        return attempts
                .filter(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        if (isLoggingEnabled) {
                            System.out.println("Checking against: "
                                    + throwable.getClass().getSimpleName()
                                    + " for strategy: "
                                    + BaseBackoffStrategy.this.getClass().getSimpleName());
                        }
                        return getRetryIfFunction().call(throwable);
                    }
                })
                .zipWith(Observable.range(1, getMaxRetries() + 1),
                        new ObservableUtils.RxPair<Throwable, Integer>())
                .doOnNext(new Action1<Pair<Throwable, Integer>>() {
                    @Override
                    public void call(Pair<Throwable, Integer> ti) {
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
                .flatMap(new Func1<Pair<Throwable, Integer>, Observable<?>>() {
                    @Override
                    public Observable<?> call(Pair<Throwable, Integer> ti) {
                        if (ti.second <= getMaxRetries()) {
                            return getWaitTime(ti.second);
                        } else {
                            return Observable.error(ti.first);
                        }
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (isLoggingEnabled) {
                            throwable.printStackTrace();
                        }
                    }
                });
    }

    public static class Builder implements Optional, AddThrowable<Builder> {
        private boolean isExclusive = false;
        private List<Class<? extends Throwable>> throwableList;
        private List<Integer> httpCodeList;
        private Func1<Throwable, Boolean> retryIfFunction;
        private Action2<Throwable, Integer> doOnRetryAction;

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
        public Builder setRetryFunction(Func1<Throwable, Boolean> retryIf) {
            this.retryIfFunction = retryIf;
            return this;
        }

        @Override
        public Builder setOnRetryAction(Action2<Throwable, Integer> onRetryAction) {
            this.doOnRetryAction = onRetryAction;
            return this;
        }
    }

    public interface Optional {

        public Optional setOnRetryAction(Action2<Throwable, Integer> onRetryAction);

        public Optional setRetryFunction(Func1<Throwable, Boolean> retryIf);
    }
}
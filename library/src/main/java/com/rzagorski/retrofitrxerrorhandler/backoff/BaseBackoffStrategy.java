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

import com.rzagorski.retrofitrxerrorhandler.utils.ObservableUtils;
import com.rzagorski.retrofitrxerrorhandler.utils.Pair;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;

/**
 * Created by Robert Zagórski on 2016-09-28.
 */
public abstract class BaseBackoffStrategy implements BackoffStrategy {

    private boolean isLoggingEnabled;

    protected abstract Observable<Long> getWaitTime(int retry);

    protected abstract int getMaxRetries();

    public abstract Func1<Throwable, Boolean> getRetryIfFunction();

    public abstract Action2<Throwable, Integer> doOnRetry(Throwable throwable, Integer integer);

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
}
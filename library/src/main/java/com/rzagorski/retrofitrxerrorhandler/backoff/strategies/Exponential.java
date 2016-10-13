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
package com.rzagorski.retrofitrxerrorhandler.backoff.strategies;

import com.rzagorski.retrofitrxerrorhandler.backoff.BaseBackoffStrategy;
import com.rzagorski.retrofitrxerrorhandler.backoff.DefaultDoOnRetryAction;
import com.rzagorski.retrofitrxerrorhandler.backoff.retryBehavior.ExclusiveRetryIfBehaviour;
import com.rzagorski.retrofitrxerrorhandler.backoff.retryBehavior.InclusiveRetryIfBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func1;

/**
 * Exponential backoff strategy. Executes retry after a specified time span defined by the
 * {@link com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Exponential.Optional#setBase(int) base}
 * and {@link com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Exponential.AddDelay#setMaxRetries(int) maximum number of retries}
 * composed in <a href="http://chubbyrevision-a2level.weebly.com/uploads/1/0/5/8/10584247/267599271_orig.gif?236">exponential function</a>.
 * <br></br>
 * Enables setting of {@code Throwables}, http codes checked, when retry is needed,
 * maximum number of retries.
 * <p></p>
 * Created by Robert Zagórski on 2016-09-28.
 */
public class Exponential extends BaseBackoffStrategy {
    private boolean isExclusive;
    private List<Class<? extends Throwable>> throwableList;
    private Func1<Throwable, Boolean> retryIfFunc;
    private Action2<Throwable, Integer> doOnRetryAction;
    private final int maxRetries;
    private final int base;

    private Exponential(Builder builder) {
        super();
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
        this.maxRetries = builder.maxRetries;
        this.base = builder.base;
    }

    public static AddThrowable init() {
        return new Builder();
    }

    @Override
    protected Observable<Long> getWaitTime(int retry) {
        return Observable.timer((long) Math.pow(base, retry), TimeUnit.SECONDS);
    }

    protected int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public Func1<Throwable, Boolean> getRetryIfFunction() {
        return retryIfFunc;
    }

    @Override
    public Action2<Throwable, Integer> doOnRetry(Throwable throwable, Integer retry) {
        return doOnRetryAction;
    }

    @Override
    public boolean isApplicable(Throwable throwable) {
        return retryIfFunc.call(throwable);
    }

    public static class Builder implements AddDelay, AddThrowable, Optional {
        private boolean isExclusive = false;
        private List<Class<? extends Throwable>> throwableList;
        private List<Integer> httpCodeList;
        private Func1<Throwable, Boolean> retryIfFunction;
        private Action2<Throwable, Integer> doOnRetryAction;
        private int maxRetries = 3;
        private int base = 2;

        private Builder() {
            throwableList = new ArrayList<>();
            httpCodeList = new ArrayList<>();
        }

        public Builder exclusive() {
            isExclusive = true;
            return this;
        }

        public Builder addThrowable(Class<? extends Throwable> throwableForBackoff) {
            this.throwableList.add(throwableForBackoff);
            return this;
        }

        public Builder setThrowable(List<Class<? extends Throwable>> throwableForBackoff) {
            this.throwableList = throwableForBackoff;
            return this;
        }

        public Builder setHttpCodeList(List<Integer> code) {
            this.httpCodeList = code;
            return this;
        }

        public Builder addHttpCode(int code) {
            this.httpCodeList.add(code);
            return this;
        }

        public Optional setRetryFunction(Func1<Throwable, Boolean> retryIf) {
            this.retryIfFunction = retryIf;
            return this;
        }

        public Optional setOnRetryAction(Action2<Throwable, Integer> onRetryAction) {
            this.doOnRetryAction = onRetryAction;
            return this;
        }

        public Builder setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public AddDelay setBase(int base) {
            this.base = base;
            return this;
        }

        public Exponential build() {
            return new Exponential(this);
        }

    }

    public interface AddDelay {

        /**
         * Sets the maximum number of retries of original request.
         *
         * @param maxRetries {@link java.lang.Integer Integer} indicating maximum number of retries.
         * @return the Builder to add more parameters
         */
        public Builder setMaxRetries(int maxRetries);
    }

    public interface AddThrowable {

        /**
         * When this flag is enabled the retry is made for everything except the parameters set
         * in this backoff strategy.
         *
         * @return the Builder to add more parameters
         */
        public Builder exclusive();

        /**
         * Adds a single {@link java.lang.Throwable} to the list of checked errors.
         *
         * @param throwableForBackoff the class of Error or Exception that extends {@link java.lang.Throwable}
         * @return the Builder to add more parameters
         */
        public Builder addThrowable(Class<? extends Throwable> throwableForBackoff);

        /**
         * Adds a list of {@link java.lang.Throwable}.
         * <br>
         * A list of previously added errors will be overriden.
         *
         * @param throwableForBackoff the list of {@link java.lang.Throwable}
         * @return the Builder to add more parameters
         */
        public Builder setThrowable(List<Class<? extends Throwable>> throwableForBackoff);

        /**
         * Adds a list of HTTP code.
         * <br>
         * A list of previously added codes will be overriden.
         *
         * @param codes the list of code of type {@link java.lang.Integer}.
         * @return the Builder to add more parameters.
         */
        public Builder setHttpCodeList(List<Integer> codes);

        /**
         * Adds a single code to the list of checked HTTP codes.
         *
         * @param code of type {@link java.lang.Integer Integer} a HTTP code to be checked taken from response.
         * @return the Builder to add more parameters
         */
        public Builder addHttpCode(int code);
    }

    public interface Optional {

        /**
         * The base of exponential function used to count backoff time
         *
         * @param base the base of exponential function
         * @return Builder object
         */
        public AddDelay setBase(int base);

        public Optional setRetryFunction(Func1<Throwable, Boolean> retryIf);

        public Optional setOnRetryAction(Action2<Throwable, Integer> onRetryAction);

        /**
         * Builds the Backoff strategy taking previously set parameters.
         *
         * @return backoff strategy of type {@link com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Exponential}
         */
        public Exponential build();
    }
}
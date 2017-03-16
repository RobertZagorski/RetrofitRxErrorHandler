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
package com.rzagorski.retrofitrx2errorhandler.backoff.strategies;

import com.rzagorski.retrofitrx2errorhandler.backoff.BaseBackoffStrategy;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;

/**
 * Exponential backoff strategy. Executes retry after a specified time span defined by the
 * {@link com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Exponential.Optional#setBase(int) base}
 * and {@link com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Exponential.AddDelay#setMaxRetries(int) maximum number of retries}
 * composed in <a href="http://chubbyrevision-a2level.weebly.com/uploads/1/0/5/8/10584247/267599271_orig.gif?236">exponential function</a>.
 * <br></br>
 * Enables setting of {@code Throwables}, http codes checked, when retry is needed,
 * maximum number of retries.
 * <p></p>
 * Created by Robert Zagórski on 2016-09-28.
 */
public class Exponential extends BaseBackoffStrategy {
    private final int maxRetries;
    private final int base;

    private Exponential(Builder builder) {
        super(builder.baseBuilder);
        this.maxRetries = builder.maxRetries;
        this.base = builder.base;
    }

    public static AddReaction<Builder> init() {
        return new Builder();
    }

    @Override
    protected Observable<Long> getWaitTime(int retry) {
        return Observable.timer((long) Math.pow(base, retry), TimeUnit.SECONDS);
    }

    protected int getMaxRetries() {
        return maxRetries;
    }


    public static class Builder implements AddReaction<Builder>, AddDelay, Optional {
        private BaseBackoffStrategy.Builder baseBuilder;
        private int maxRetries = 3;
        private int base = 2;

        private Builder() {
            baseBuilder = new BaseBackoffStrategy.Builder();
        }

        @Override
        public BaseBackoffStrategy.Optional setOnRetryAction(BiConsumer<Throwable, Integer> onRetryAction) {
            baseBuilder.setOnRetryAction(onRetryAction);
            return this;
        }

        @Override
        public BaseBackoffStrategy.Optional setRetryFunction(Function<Throwable, Boolean> retryIf) {
            baseBuilder.setRetryFunction(retryIf);
            return this;
        }

        @Override
        public Builder exclusive() {
            baseBuilder.exclusive();
            return this;
        }

        @Override
        public Builder addThrowable(Class<? extends Throwable> throwableForBackoff) {
            baseBuilder.addThrowable(throwableForBackoff);
            return this;
        }

        @Override
        public Builder setThrowable(List<Class<? extends Throwable>> throwableForBackoffList) {
            baseBuilder.setThrowable(throwableForBackoffList);
            return this;
        }

        @Override
        public Builder setHttpCodeList(List<Integer> codes) {
            baseBuilder.setHttpCodeList(codes);
            return this;
        }

        @Override
        public Builder addHttpCode(int code) {
            baseBuilder.addHttpCode(code);
            return this;
        }

        @Override
        public Builder addObservable(Observable<?> observable) {
            baseBuilder.addObservable(observable);
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

    public interface Optional extends BaseBackoffStrategy.Optional {

        /**
         * The base of exponential function used to count backoff time
         *
         * @param base the base of exponential function
         * @return Builder object
         */
        public AddDelay setBase(int base);

        /**
         * Builds the Backoff strategy taking previously set parameters.
         *
         * @return backoff strategy of type {@link com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Exponential}
         */
        public Exponential build();
    }
}
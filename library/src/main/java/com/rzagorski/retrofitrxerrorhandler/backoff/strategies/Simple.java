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

import java.util.List;

import rx.Observable;

/**
 * Simple backoff strategy. Executes retry immediately.
 * <br>
 * Enables setting of {@code Throwables}, http codes checked, when retry is needed,
 * maximum number of retries.
 * <p>
 * Created by Robert Zagórski on 2016-09-28.
 */
public class Simple extends BaseBackoffStrategy {
    private final int maxRetries;

    private Simple(Builder builder) {
        super(builder.baseBuilder);
        this.maxRetries = builder.maxRetries;
    }

    public static AddThrowable<Builder> init() {
        return new Builder();
    }

    @Override
    protected Observable<Long> getWaitTime(int retry) {
        return Observable.just(0L);
    }

    protected int getMaxRetries() {
        return maxRetries;
    }

    public static class Builder implements AddThrowable<Builder>, Optional {
        private BaseBackoffStrategy.Builder baseBuilder;
        private int maxRetries = 3;

        private Builder() {
            baseBuilder = new BaseBackoffStrategy.Builder();
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

        public Builder setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Simple build() {
            return new Simple(this);
        }
    }

    public interface Optional {

        /**
         * Sets the maximum number of retries of original request.
         *
         * @param maxRetries {@link java.lang.Integer Integer} indicating maximum number of retries.
         * @return the Builder to add more parameters
         */
        public Builder setMaxRetries(int maxRetries);

        /**
         * Builds the Backoff strategy taking previously set parameters.
         *
         * @return backoff strategy of type {@link com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Simple}
         */
        public Simple build();
    }
}

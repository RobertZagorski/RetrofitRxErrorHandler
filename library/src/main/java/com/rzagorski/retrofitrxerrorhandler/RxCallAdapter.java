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
package com.rzagorski.retrofitrxerrorhandler;

import com.rzagorski.retrofitrxerrorhandler.backoff.BackoffStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder for <a href="https://github.com/square/retrofit/blob/master/retrofit/src/main/java/retrofit2/CallAdapter.java#L62">CallAdapter.Factory</a>
 * Allows for adding {@link BackoffStrategy backoffStrategies} and logging feature.
 * <br>
 * Created by Robert Zagórski on 2016-09-28.
 */
public class RxCallAdapter {
    private List<BackoffStrategy> backoffStrategyList;
    private boolean loggingEnabled;

    private RxCallAdapter(Builder builder) {
        this.backoffStrategyList = builder.backoffStrategyList;
        this.loggingEnabled = builder.loggingEnabled;
    }

    public List<BackoffStrategy> getBackoffStrategies() {
        return backoffStrategyList;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public static final class Builder {
        private List<BackoffStrategy> backoffStrategyList;
        private boolean loggingEnabled;

        public Builder() {
            backoffStrategyList = new ArrayList<>();
        }

        public Builder setBackoffStrategy(List<BackoffStrategy> backoffStrategies) {
            this.backoffStrategyList = backoffStrategies;
            return this;
        }

        public Builder addBackoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategyList.add(backoffStrategy);
            return this;
        }

        public Builder setLoggingEnabled(boolean loggingEnabled) {
            this.loggingEnabled = loggingEnabled;
            return this;
        }

        public RxCallAdapter build() {
            return new RxCallAdapter(this);
        }
    }
}

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
package com.rzagorski.retrofitrxerrorhandler.backoff.retryBehavior;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Func1;

/**
 * <br>
 * Created by Robert Zagórski on 2016-10-10.
 */
public abstract class BaseRetryIfBehavior implements Func1<Throwable, Boolean> {
    protected List<Class<? extends Throwable>> throwableList = new ArrayList<>();
    protected List<Integer> httpCodesList = new ArrayList<>();

    public BaseRetryIfBehavior(List<Class<? extends Throwable>> throwableList, List<Integer> httpCodesList) {
        this.throwableList = throwableList;
        this.httpCodesList = httpCodesList;
    }
}

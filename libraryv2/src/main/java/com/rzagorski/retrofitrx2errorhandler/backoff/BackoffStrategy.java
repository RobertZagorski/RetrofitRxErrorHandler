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

import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * The base interface for all backoff strategies.
 * <p>
 * <br>
 * Created by Robert Zagórski on 2016-09-28.
 */
public interface BackoffStrategy extends Function<Observable<? extends Throwable>, Observable<?>> {

    /**
     * Defines, whether the specifies throwable should be handled by this {@link BackoffStrategy}
     *
     * @param throwable
     * @return
     */
    boolean isApplicable(Throwable throwable) throws Exception;

    /**
     * Enabling logging feature for this {@link BackoffStrategy}
     *
     * @param logging
     */
    void setLoggingEnabled(boolean logging);

    <T> Observable<T> getBackupObservable();
}

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
package com.rzagorski.retrofitrx2errorhandler.backoff.retryBehavior;

import java.util.List;

import retrofit2.HttpException;

public class InclusiveRetryIfBehaviour extends BaseRetryIfBehavior {

    public InclusiveRetryIfBehaviour(List<Class<? extends Throwable>> throwableList, List<Integer> httpCodesList) {
        super(throwableList, httpCodesList);
    }

    @Override
    public Boolean apply(Throwable throwable) {
        boolean result = false;
        if (httpCodesList != null) {
            if (HttpException.class.isInstance(throwable)) {
                result = checkHttpCodes((HttpException) throwable);
            }
        }
        return result || checkError(throwable);
    }

    private boolean checkHttpCodes(HttpException httpException) {
        int errorCode = httpException.code();
        for (Integer httpCode : httpCodesList) {
            if (httpCode.equals(errorCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkError(Throwable throwable) {
        for (Class<? extends Throwable> throwableFromList : throwableList) {
            if (throwableFromList.isInstance(throwable)) {
                return true;
            }
        }
        return false;
    }
}
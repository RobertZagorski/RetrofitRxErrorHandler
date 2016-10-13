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

import rx.functions.Action2;

/**
 * Created by Robert Zagórski on 2016-10-06.
 */

public class DefaultDoOnRetryAction implements Action2<Throwable, Integer> {
    @Override
    public void call(Throwable throwable, Integer retry) {
        System.out.println(throwable + " occurred on " + retry + " retry");
    }
}
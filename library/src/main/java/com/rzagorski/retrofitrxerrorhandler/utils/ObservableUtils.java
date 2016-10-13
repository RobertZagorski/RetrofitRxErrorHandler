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
package com.rzagorski.retrofitrxerrorhandler.utils;


import rx.functions.Func2;

/**
 * Created by Robert Zagórski on 2016-02-04.
 */
public class ObservableUtils {

    public static class RxPair<T1, T2> implements Func2<T1, T2, Pair<T1, T2>> {
        @Override
        public Pair<T1, T2> call(T1 t1, T2 t2) {
            return Pair.of(t1, t2);
        }
    }
}
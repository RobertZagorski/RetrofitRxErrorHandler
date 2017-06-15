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
package com.rzagorski.retrofitrx2errorhandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * Main class for communication with <a href="https://github.com/square/retrofit/blob/master/retrofit/src/main/java/retrofit2/CallAdapter.java#L62">CallAdapter.Factory</a>.
 * Wraps the call made by
 * <a href="https://github.com/square/retrofit/blob/master/retrofit-adapters/rxjava/src/main/java/retrofit2/adapter/rxjava/RxJavaCallAdapterFactory.java">RxJavaCallAdapterFactory</a>
 * and uses <a href="https://github.com/ReactiveX/RxJava/blob/1.x/src/main/java/rx/Observable.java#L277">Observable#compose(Observable.Transformer)</a>
 * to pass handling of events made with call to it's inheritors.
 * <br>
 * Created by Robert Zagórski on 2016-09-28.
 */

abstract class BaseRxCallAdapterFactory extends CallAdapter.Factory {

    private final RxJava2CallAdapterFactory original;

    BaseRxCallAdapterFactory() {
        original = RxJava2CallAdapterFactory.create();
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return new RxCallAdapterWrapper(retrofit, original.get(returnType, annotations, retrofit));
    }

    private class RxCallAdapterWrapper<R> implements CallAdapter<R, Object> {
        private final Retrofit retrofit;
        private final CallAdapter<R, R> wrapped;

        RxCallAdapterWrapper(Retrofit retrofit, CallAdapter<R, R> wrapped) {
            this.retrofit = retrofit;
            this.wrapped = wrapped;
        }

        @Override
        public Type responseType() {
            return wrapped.responseType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object adapt(Call<R> call) {
            return ((Observable<R>) wrapped.adapt(call))
                    .compose(transformRequest());
        }
    }

    protected abstract <T> ObservableTransformer<T, T> transformRequest();
}

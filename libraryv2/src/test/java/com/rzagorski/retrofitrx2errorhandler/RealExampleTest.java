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

import com.rzagorski.retrofitrx2errorhandler.backoff.strategies.Exponential;
import com.rzagorski.retrofitrx2errorhandler.model.GitHub;
import com.rzagorski.retrofitrx2errorhandler.model.Repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Observer;
import io.reactivex.observers.DefaultObserver;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import io.reactivex.Observable;

@RunWith(MockitoJUnitRunner.class)
public class RealExampleTest {

    public static final String API_URL = "https://api.github.com";

    /**
     * This test executes the real query to github server.
     * Test created by Robert Zagorski on 19.10.2016
     */
    @Test
    public void main() throws IOException, InterruptedException {
        // Create a very simple REST adapter which points the GitHub API.
        RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                .addBackoffStrategy(Exponential.init()
                        .addThrowable(UnknownHostException.class)
                        .addThrowable(SocketTimeoutException.class)
                        .setMaxRetries(3).build())
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(new RxErrorHandingFactory(rxCallAdapter))
                .build();

        // Create an instance of our GitHub API interface.
        GitHub github = retrofit.create(GitHub.class);

        // Create a call instance for looking up Retrofit contributors.
        Observable<List<Repository>> call = github.repos("square");

        final CountDownLatch latch = new CountDownLatch(1);
        // Fetch and print a list of the contributors to the retrofiterrorhandler.
        call.subscribe(new DefaultObserver<List<Repository>>() {
            @Override
            public void onComplete() {
                System.out.println(new GregorianCalendar().toInstant().toString() + " Finished");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                System.out.println(new GregorianCalendar().toInstant().toString() + " Finished with error: " + e);
                onComplete();
            }

            @Override
            public void onNext(List<Repository> repositories) {
                for (Repository repository : repositories) {
                    System.out.println(repository.name + " (" + repository.description + ")");
                }
            }
        });
        latch.await();
    }
}
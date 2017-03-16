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
package com.rzagorski.retrofitrx2errorhandler.utils;

import com.google.gson.reflect.TypeToken;
import com.rzagorski.retrofitrx2errorhandler.model.GitHub;
import com.rzagorski.retrofitrx2errorhandler.model.Repository;

import java.io.IOException;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Robert Zagórski on 2016-10-03.
 */

public class MockWebServerUtils {

    public static final Long ONE_SEC = 1000L;

    public static GitHub createRetrofitInstance(String url, CallAdapter.Factory factory) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(factory)
                .build();
        return retrofit.create(GitHub.class);
    }

    public static MockResponse getSuccessfulResponse() throws InterruptedException {
        try {
            String repositories = new FileUtils().loadJSON("src/test/resources/response.json",
                    new TypeToken<List<Repository>>() {
                    }.getType());
            return new MockResponse()
                    .setResponseCode(200)
                    .setBody(repositories);
        } catch (IOException e) {
            System.err.println("Could not load successful response. Make sure the test execution path is proper");
            e.printStackTrace();
            throw new InterruptedException("Could not load successful response. Make sure the test execution path is proper");
        }
    }
}

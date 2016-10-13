package com.rzagorski.retrofitrxerrorhandler.utils;

import com.google.gson.reflect.TypeToken;
import com.rzagorski.retrofitrxerrorhandler.model.GitHub;
import com.rzagorski.retrofitrxerrorhandler.model.Repository;

import java.io.FileNotFoundException;
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

    public static MockResponse getSuccessfulResponse() throws FileNotFoundException, IOException {
        String repositories = new FileUtils().loadJSON("src/test/resources/response.json",
                new TypeToken<List<Repository>>() {
                }.getType());
        return new MockResponse()
                .setResponseCode(200)
                .setBody(repositories);
    }
}

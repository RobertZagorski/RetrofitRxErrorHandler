package com.rzagorski.retrofitrx2errorhandler.model;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

public interface GitHub {
    @GET("/users/{owner}/repos")
    Observable<List<Repository>> repos(
            @Path("owner") String owner);
}
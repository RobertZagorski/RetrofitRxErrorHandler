package com.rzagorski.retrofitrxerrorhandler;

import com.rzagorski.retrofitrxerrorhandler.backoff.strategies.Exponential;
import com.rzagorski.retrofitrxerrorhandler.model.GitHub;
import com.rzagorski.retrofitrxerrorhandler.model.Repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;

@RunWith(MockitoJUnitRunner.class)
public class RealExampleTest {

    public static final String API_URL = "https://api.github.com";

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
        call.subscribe(new Subscriber<List<Repository>>() {
            @Override
            public void onCompleted() {
                System.out.println(new GregorianCalendar().toInstant().toString() + " Finished");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                System.out.println(new GregorianCalendar().toInstant().toString() + " Finished with error: " + e);
                onCompleted();
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
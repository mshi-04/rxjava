package com.example.rxjava.di;

import com.example.rxjava.data.remote.ForecastApi;
import com.example.rxjava.data.repository.RemoteForecastRepository;
import com.example.rxjava.domain.repository.ForecastRepository;
import com.example.rxjava.domain.usecase.GetForecastUseCase;
import com.squareup.moshi.Moshi;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public final class ServiceLocator {

    private static final String BASE_URL = "https://www.7timer.info/";
    private static final long TIMEOUT_SECONDS = 10;

    private static volatile ServiceLocator instance;

    private final GetForecastUseCase getForecastUseCase;

    private ServiceLocator() {
        ForecastRepository repository = new RemoteForecastRepository(createForecastApi());
        getForecastUseCase = new GetForecastUseCase(repository);
    }

    public static ServiceLocator getInstance() {
        if (instance != null) {
            return instance;
        }

        synchronized (ServiceLocator.class) {
            if (instance == null) {
                instance = new ServiceLocator();
            }
            return instance;
        }
    }

    public GetForecastUseCase getForecastUseCase() {
        return getForecastUseCase;
    }

    private static ForecastApi createForecastApi() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                // createAsync は独自にスレッドを起こし、呼び出し側の subscribeOn の効果が見えなくなる
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(new Moshi.Builder().build()))
                .build()
                .create(ForecastApi.class);
    }
}

package com.example.rxjava.data.remote;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ForecastApi {

    @GET("bin/api.pl?product=civillight&output=json")
    Single<ForecastResponse> dailyForecast(
            @Query("lat") double latitude,
            @Query("lon") double longitude);
}

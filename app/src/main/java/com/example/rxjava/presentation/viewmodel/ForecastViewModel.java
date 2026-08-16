package com.example.rxjava.presentation.viewmodel;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.example.rxjava.di.ServiceLocator;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.usecase.GetForecastUseCase;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ForecastViewModel extends ViewModel {

    public final ObservableBoolean loading = new ObservableBoolean(false);
    public final ObservableField<List<DailyForecast>> forecasts = new ObservableField<>();
    public final ObservableBoolean hasError = new ObservableBoolean(false);

    private final GetForecastUseCase getForecast = ServiceLocator.getInstance().getForecastUseCase();

    private Disposable disposable;

    public void load() {
        disposeCurrent();
        hasError.set(false);
        loading.set(true);
        disposable = getForecast.invoke(Location.TOKYO)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSuccess, this::handleError);
    }

    private void handleSuccess(List<DailyForecast> result) {
        loading.set(false);
        forecasts.set(result);
    }

    private void handleError(Throwable error) {
        loading.set(false);
        hasError.set(true);
    }

    private void disposeCurrent() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }

    @Override
    protected void onCleared() {
        disposeCurrent();
    }
}

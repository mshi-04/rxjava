package com.example.rxjava.presentation.viewmodel;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.usecase.GetForecastUseCase;
import com.example.rxjava.presentation.util.UiSchedulers;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public final class ForecastViewModel extends ViewModel {

    private static final String TAG = "ForecastViewModel";

    public final ObservableField<ForecastUiState> state = new ObservableField<>();

    private final GetForecastUseCase getForecastUseCase;
    private final UiSchedulers schedulers;

    private Disposable disposable;

    @Inject
    public ForecastViewModel(GetForecastUseCase getForecastUseCase, UiSchedulers schedulers) {
        this.getForecastUseCase = getForecastUseCase;
        this.schedulers = schedulers;
        load();
    }

    @MainThread
    public void load() {
        disposeCurrent();
        state.set(new ForecastUiState.Loading());
        disposable = getForecastUseCase.invoke(Location.TOKYO)
                .subscribeOn(schedulers.background())
                .observeOn(schedulers.main())
                .subscribe(this::publishSuccess, this::publishError);
    }

    // onSuccess が投げた例外は onError へ渡らず RxJavaPlugins.onError で落ちる。
    // state.set は Activity の render と Adapter まで同期的に呼ぶため、その連鎖もここで受ける
    private void publishSuccess(List<DailyForecast> result) {
        try {
            state.set(new ForecastUiState.Success(result));
        } catch (RuntimeException failedToPublish) {
            publishError(failedToPublish);
        }
    }

    private void publishError(Throwable error) {
        Log.w(TAG, "予報を取得できなかった", error);
        state.set(new ForecastUiState.Failure());
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

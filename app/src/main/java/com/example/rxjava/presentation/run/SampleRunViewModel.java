package com.example.rxjava.presentation.run;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.example.rxjava.di.ServiceLocator;
import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.model.SampleLogLine;
import com.example.rxjava.domain.usecase.RunSampleUseCase;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

public class SampleRunViewModel extends ViewModel {

    public final ObservableField<String> logText = new ObservableField<>("");
    public final ObservableBoolean running = new ObservableBoolean(false);

    private final RunSampleUseCase runSample = ServiceLocator.getInstance().getRunSampleUseCase();
    private final StringBuilder buffer = new StringBuilder();

    private Disposable disposable;

    public void start(Sample sample) {
        stop();
        buffer.setLength(0);
        logText.set("");
        running.set(true);
        disposable = runSample.invoke(sample.getId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::appendLine, this::appendError, this::markFinished);
    }

    public void stop() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
        running.set(false);
    }

    private void appendLine(SampleLogLine line) {
        buffer.append('[').append(line.getThreadName()).append("] ")
                .append(line.getMessage()).append('\n');
        logText.set(buffer.toString());
    }

    private void appendError(Throwable error) {
        buffer.append("ストリームが異常終了: ").append(error).append('\n');
        logText.set(buffer.toString());
        markFinished();
    }

    private void markFinished() {
        running.set(false);
    }

    @Override
    protected void onCleared() {
        stop();
    }
}

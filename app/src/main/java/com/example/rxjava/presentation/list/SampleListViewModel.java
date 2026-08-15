package com.example.rxjava.presentation.list;

import androidx.lifecycle.ViewModel;

import com.example.rxjava.di.ServiceLocator;
import com.example.rxjava.domain.model.Sample;

import java.util.List;

public class SampleListViewModel extends ViewModel {

    private final List<Sample> samples =
            ServiceLocator.getInstance().getSampleListUseCase().invoke();

    public List<Sample> getSamples() {
        return samples;
    }
}

package com.example.rxjava.presentation.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rxjava.R;
import com.example.rxjava.databinding.ActivityForecastBinding;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.presentation.adapter.ForecastAdapter;
import com.example.rxjava.presentation.viewmodel.ForecastViewModel;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForecastActivity extends AppCompatActivity {

    private final ForecastAdapter adapter = new ForecastAdapter();

    private ForecastViewModel viewModel;
    private Observable.OnPropertyChangedCallback forecastsCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityForecastBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_forecast);
        viewModel = new ViewModelProvider(this).get(ForecastViewModel.class);
        binding.setViewModel(viewModel);

        binding.forecastList.setLayoutManager(new LinearLayoutManager(this));
        binding.forecastList.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.forecastList.setAdapter(adapter);

        forecastsCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                submitForecasts();
            }
        };
        viewModel.forecasts.addOnPropertyChangedCallback(forecastsCallback);
        // 回転後は ViewModel が値を保持済みで変更通知が来ないため、登録直後に現在値を流す
        submitForecasts();

        if (savedInstanceState == null) {
            viewModel.load();
        }
    }

    @Override
    protected void onDestroy() {
        viewModel.forecasts.removeOnPropertyChangedCallback(forecastsCallback);
        super.onDestroy();
    }

    private void submitForecasts() {
        List<DailyForecast> result = viewModel.forecasts.get();
        if (result == null) {
            return;
        }
        adapter.submit(result);
    }
}

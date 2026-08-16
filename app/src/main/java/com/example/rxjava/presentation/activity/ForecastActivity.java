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

public class ForecastActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityForecastBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_forecast);
        ForecastViewModel viewModel = new ViewModelProvider(this).get(ForecastViewModel.class);
        binding.setViewModel(viewModel);

        ForecastAdapter adapter = new ForecastAdapter();
        binding.forecastList.setLayoutManager(new LinearLayoutManager(this));
        binding.forecastList.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.forecastList.setAdapter(adapter);

        viewModel.forecasts.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                List<DailyForecast> result = viewModel.forecasts.get();
                if (result != null) {
                    adapter.submit(result);
                }
            }
        });

        if (savedInstanceState == null) {
            viewModel.load();
        }
    }
}

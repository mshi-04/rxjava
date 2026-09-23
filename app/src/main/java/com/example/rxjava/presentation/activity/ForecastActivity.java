package com.example.rxjava.presentation.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rxjava.R;
import com.example.rxjava.databinding.ActivityForecastBinding;
import com.example.rxjava.presentation.adapter.ForecastAdapter;
import com.example.rxjava.presentation.viewmodel.ForecastUiState;
import com.example.rxjava.presentation.viewmodel.ForecastViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForecastActivity extends AppCompatActivity {

    private final ForecastAdapter adapter = new ForecastAdapter();

    private ActivityForecastBinding binding;
    private ForecastViewModel viewModel;
    private Observable.OnPropertyChangedCallback stateCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_forecast);
        viewModel = new ViewModelProvider(this).get(ForecastViewModel.class);
        binding.setViewModel(viewModel);

        binding.forecastList.setLayoutManager(new LinearLayoutManager(this));
        binding.forecastList.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.forecastList.setAdapter(adapter);

        stateCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                render(viewModel.state.get());
            }
        };
        viewModel.state.addOnPropertyChangedCallback(stateCallback);
        // 回転後は ViewModel が状態を保持済みで変更通知が来ないため、登録直後に現在の状態を描画する
        render(viewModel.state.get());
    }

    @Override
    protected void onDestroy() {
        if (viewModel != null && stateCallback != null) {
            viewModel.state.removeOnPropertyChangedCallback(stateCallback);
        }
        super.onDestroy();
    }

    private void render(ForecastUiState state) {
        binding.loadingIndicator.setVisibility(visibleIf(state instanceof ForecastUiState.Loading));
        binding.forecastList.setVisibility(visibleIf(state instanceof ForecastUiState.Success));
        binding.errorContainer.setVisibility(visibleIf(state instanceof ForecastUiState.Failure));
        if (state instanceof ForecastUiState.Success success) {
            adapter.submit(success.forecasts());
        }
    }

    private static int visibleIf(boolean visible) {
        return visible ? View.VISIBLE : View.GONE;
    }
}

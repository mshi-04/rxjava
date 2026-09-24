package com.example.rxjava.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rxjava.databinding.FragmentForecastBinding;
import com.example.rxjava.presentation.adapter.ForecastAdapter;
import com.example.rxjava.presentation.viewmodel.ForecastUiState;
import com.example.rxjava.presentation.viewmodel.ForecastViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForecastFragment extends Fragment {

    private ForecastViewModel viewModel;
    private FragmentForecastBinding binding;
    private ForecastAdapter adapter;
    private Observable.OnPropertyChangedCallback stateCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ForecastViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentForecastBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setViewModel(viewModel);

        adapter = new ForecastAdapter();
        binding.forecastList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.forecastList.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        binding.forecastList.setAdapter(adapter);

        stateCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                render(viewModel.state.get());
            }
        };
        viewModel.state.addOnPropertyChangedCallback(stateCallback);
        // View の再生成時は状態変更通知が来ないため、現在値を直ちに描画する
        render(viewModel.state.get());
    }

    @Override
    public void onDestroyView() {
        viewModel.state.removeOnPropertyChangedCallback(stateCallback);
        binding.forecastList.setAdapter(null);
        binding.unbind();
        stateCallback = null;
        adapter = null;
        binding = null;
        super.onDestroyView();
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

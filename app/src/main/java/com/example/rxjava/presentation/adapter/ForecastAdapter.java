package com.example.rxjava.presentation.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rxjava.databinding.ItemForecastBinding;
import com.example.rxjava.domain.model.DailyForecast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private List<DailyForecast> forecasts = Collections.emptyList();

    public void submit(List<DailyForecast> newForecasts) {
        forecasts = new ArrayList<>(newForecasts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemForecastBinding binding = ItemForecastBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ForecastViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        holder.bind(forecasts.get(position));
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {

        private final ItemForecastBinding binding;

        ForecastViewHolder(ItemForecastBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DailyForecast forecast) {
            binding.setForecast(forecast);
            binding.executePendingBindings();
        }
    }
}

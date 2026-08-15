package com.example.rxjava.presentation.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rxjava.databinding.ItemSampleBinding;
import com.example.rxjava.domain.model.Sample;

import java.util.List;

public class SampleListAdapter extends RecyclerView.Adapter<SampleListAdapter.SampleViewHolder> {

    public interface OnSampleSelected {

        void onSampleSelected(Sample sample);
    }

    private final List<Sample> samples;
    private final OnSampleSelected listener;

    public SampleListAdapter(List<Sample> samples, OnSampleSelected listener) {
        this.samples = samples;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SampleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSampleBinding binding = ItemSampleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SampleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SampleViewHolder holder, int position) {
        Sample sample = samples.get(position);
        holder.bind(sample, view -> listener.onSampleSelected(sample));
    }

    @Override
    public int getItemCount() {
        return samples.size();
    }

    static class SampleViewHolder extends RecyclerView.ViewHolder {

        private final ItemSampleBinding binding;

        SampleViewHolder(ItemSampleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Sample sample, View.OnClickListener clickListener) {
            binding.setSample(sample);
            binding.setClickListener(clickListener);
            binding.executePendingBindings();
        }
    }
}

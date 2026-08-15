package com.example.rxjava.presentation.list;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rxjava.R;
import com.example.rxjava.databinding.ActivityMainBinding;
import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.presentation.run.SampleActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        SampleListViewModel viewModel = new ViewModelProvider(this).get(SampleListViewModel.class);

        binding.sampleList.setLayoutManager(new LinearLayoutManager(this));
        binding.sampleList.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.sampleList.setAdapter(new SampleListAdapter(viewModel.getSamples(), this::openSample));
    }

    private void openSample(Sample sample) {
        startActivity(SampleActivity.createIntent(this, sample.getId()));
    }
}

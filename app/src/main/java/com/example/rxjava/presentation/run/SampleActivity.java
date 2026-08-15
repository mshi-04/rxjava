package com.example.rxjava.presentation.run;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.rxjava.R;
import com.example.rxjava.databinding.ActivitySampleBinding;
import com.example.rxjava.di.ServiceLocator;
import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.model.SampleId;

public class SampleActivity extends AppCompatActivity {

    private static final String EXTRA_SAMPLE_ID = "sample_id";

    public static Intent createIntent(Context context, SampleId sampleId) {
        return new Intent(context, SampleActivity.class)
                .putExtra(EXTRA_SAMPLE_ID, sampleId.value());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SampleId sampleId = SampleId.of(getIntent().getStringExtra(EXTRA_SAMPLE_ID));
        Sample sample = ServiceLocator.getInstance().getSampleUseCase().invoke(sampleId);

        ActivitySampleBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_sample);
        binding.setSample(sample);
        binding.setViewModel(new ViewModelProvider(this).get(SampleRunViewModel.class));

        setTitle(sample.getTitle());
    }
}

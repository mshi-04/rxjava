package com.example.rxjava.presentation.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.rxjava.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForecastActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataBindingUtil.setContentView(this, R.layout.activity_forecast);
    }
}

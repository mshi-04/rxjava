package com.example.rxjava.domain.repository;

import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.model.SampleId;
import com.example.rxjava.domain.model.SampleLogLine;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;

public interface SampleRepository {

    List<Sample> findAll();

    Sample findById(SampleId id);

    Observable<SampleLogLine> run(SampleId id);
}

package com.example.rxjava.data.catalog;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.model.SampleId;
import com.example.rxjava.domain.model.SampleLogLine;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.observers.TestObserver;

class SampleCatalogTest {

    @Test
    void catalogIsNotEmpty() {
        assertFalse(SampleCatalog.definitions().isEmpty());
    }

    @Test
    void sampleIdsAreUnique() {
        List<SampleDefinition> definitions = SampleCatalog.definitions();
        Set<SampleId> ids = new HashSet<>();
        for (SampleDefinition definition : definitions) {
            ids.add(definition.getId());
        }
        assertEquals(definitions.size(), ids.size());
    }

    @Test
    void titlesAreUnique() {
        List<SampleDefinition> definitions = SampleCatalog.definitions();
        Set<String> titles = new HashSet<>();
        for (SampleDefinition definition : definitions) {
            titles.add(definition.toSample().getTitle());
        }
        assertEquals(definitions.size(), titles.size());
    }

    @Test
    void everyDefinitionToSampleCarriesConsistentData() {
        assertAll(SampleCatalog.definitions().stream().map(definition -> () -> {
            Sample sample = definition.toSample();
            assertEquals(definition.getId(), sample.getId());
            assertFalse(sample.getTitle().isBlank());
            assertFalse(sample.getDescription().isBlank());
        }));
    }

    @Test
    void basicObservableEmitsFilteredAndMappedValuesThenCompletes() {
        List<SampleLogLine> values = runToCompletion("basic-observable");

        assertAll(
                () -> assertEquals(4, values.size()),
                () -> assertEquals("onNext: 10", values.get(0).getMessage()),
                () -> assertEquals("onNext: 30", values.get(1).getMessage()),
                () -> assertEquals("onNext: 50", values.get(2).getMessage()),
                () -> assertEquals("onComplete", values.get(3).getMessage()));
    }

    @Test
    void behaviorSubjectDeliversTheLatestValueToLateSubscribers() {
        List<SampleLogLine> values = runToCompletion("behavior-subject");

        assertAll(
                () -> assertEquals(5, values.size()),
                () -> assertEquals("購読A: 初期値", values.get(0).getMessage()),
                () -> assertEquals("購読A: 1 回目の更新", values.get(1).getMessage()),
                () -> assertEquals("購読B: 1 回目の更新", values.get(2).getMessage()),
                () -> assertEquals("購読A: 2 回目の更新", values.get(3).getMessage()),
                () -> assertEquals("購読B: 2 回目の更新", values.get(4).getMessage()));
    }

    private static List<SampleLogLine> runToCompletion(String id) {
        TestObserver<SampleLogLine> observer = findById(id).execute().test();
        observer.awaitDone(5, TimeUnit.SECONDS);
        observer.assertNoErrors();
        observer.assertComplete();
        return observer.values();
    }

    private static SampleDefinition findById(String id) {
        for (SampleDefinition definition : SampleCatalog.definitions()) {
            if (definition.getId().equals(SampleId.of(id))) {
                return definition;
            }
        }
        throw new AssertionError("sample not found: " + id);
    }
}

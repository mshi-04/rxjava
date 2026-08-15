package com.example.rxjava.data.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.rxjava.data.catalog.SampleDefinition;
import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.model.SampleId;
import com.example.rxjava.domain.model.SampleLogLine;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Observable;

class InMemorySampleRepositoryTest {

    @Test
    void findAllReturnsOneSamplePerDefinitionInRegistrationOrder() {
        InMemorySampleRepository repository = new InMemorySampleRepository(Arrays.asList(
                definition("first", "First", "First description"),
                definition("second", "Second", "Second description"),
                definition("third", "Third", "Third description")));

        List<Sample> samples = repository.findAll();

        assertAll(
                () -> assertEquals(3, samples.size()),
                () -> assertEquals(SampleId.of("first"), samples.get(0).getId()),
                () -> assertEquals(SampleId.of("second"), samples.get(1).getId()),
                () -> assertEquals(SampleId.of("third"), samples.get(2).getId()));
    }

    @Test
    void findByIdReturnsTheMatchingSample() {
        InMemorySampleRepository repository = new InMemorySampleRepository(Arrays.asList(
                definition("first", "First", "First description"),
                definition("second", "Second", "Second description")));

        Sample sample = repository.findById(SampleId.of("second"));

        assertAll(
                () -> assertEquals(SampleId.of("second"), sample.getId()),
                () -> assertEquals("Second", sample.getTitle()),
                () -> assertEquals("Second description", sample.getDescription()));
    }

    @Test
    void findByIdThrowsForUnknownSampleId() {
        InMemorySampleRepository repository = new InMemorySampleRepository(
                Arrays.asList(definition("first", "First", "First description")));

        assertThrows(IllegalArgumentException.class,
                () -> repository.findById(SampleId.of("missing")));
    }

    @Test
    void runDoesNotExecuteTheBodyUntilSubscribed() {
        AtomicInteger executions = new AtomicInteger();
        InMemorySampleRepository repository = repositoryCounting(executions);

        Observable<SampleLogLine> observable = repository.run(SampleId.of("counting"));
        assertEquals(0, executions.get());

        observable.test();
        assertEquals(1, executions.get());
    }

    @Test
    void runCreatesFreshStatePerSubscription() {
        AtomicInteger executions = new AtomicInteger();
        InMemorySampleRepository repository = repositoryCounting(executions);

        Observable<SampleLogLine> observable = repository.run(SampleId.of("counting"));
        observable.test();
        observable.test();

        assertEquals(2, executions.get());
    }

    private static InMemorySampleRepository repositoryCounting(AtomicInteger executions) {
        SampleDefinition definition = new SampleDefinition(
                SampleId.of("counting"), "Counting", "Counting description",
                () -> {
                    executions.incrementAndGet();
                    return Observable.empty();
                });
        return new InMemorySampleRepository(Arrays.asList(definition));
    }

    private static SampleDefinition definition(String id, String title, String description) {
        return new SampleDefinition(SampleId.of(id), title, description, Observable::empty);
    }
}

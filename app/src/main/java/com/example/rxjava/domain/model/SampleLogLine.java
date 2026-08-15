package com.example.rxjava.domain.model;

import java.util.Objects;

public final class SampleLogLine {

    private final String threadName;
    private final String message;

    private SampleLogLine(String threadName, String message) {
        this.threadName = threadName;
        this.message = message;
    }

    public static SampleLogLine onCurrentThread(String message) {
        return new SampleLogLine(Thread.currentThread().getName(), Objects.requireNonNull(message));
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }
}

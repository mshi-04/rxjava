package com.example.rxjava.presentation.util;

import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class UiSchedulers {

    private final Scheduler background;
    private final Scheduler main;

    public UiSchedulers(Scheduler background, Scheduler main) {
        this.background = Objects.requireNonNull(background, "background");
        this.main = Objects.requireNonNull(main, "main");
    }

    public static UiSchedulers android() {
        return new UiSchedulers(Schedulers.io(), AndroidSchedulers.mainThread());
    }

    public Scheduler background() {
        return background;
    }

    public Scheduler main() {
        return main;
    }
}

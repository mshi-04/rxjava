package com.example.rxjava.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ForecastDateTest {

    @Test
    void of_validDate_keepsYearMonthAndDay() {
        // Act
        ForecastDate date = ForecastDate.of(2026, 8, 16);

        // Assert
        assertAll(
                () -> assertEquals(2026, date.getYear()),
                () -> assertEquals(8, date.getMonth()),
                () -> assertEquals(16, date.getDay()));
    }

    @Test
    void of_bothBounds_areAccepted() {
        // Act
        ForecastDate first = ForecastDate.of(2026, 1, 1);
        ForecastDate last = ForecastDate.of(2026, 12, 31);

        // Assert
        assertAll(
                () -> assertEquals(1, first.getMonth()),
                () -> assertEquals(1, first.getDay()),
                () -> assertEquals(12, last.getMonth()),
                () -> assertEquals(31, last.getDay()));
    }

    @Test
    void of_yearOutsideRange_throwsIllegalArgument() {
        // Arrange
        Executable tooEarly = () -> ForecastDate.of(1899, 8, 16);
        Executable tooLate = () -> ForecastDate.of(3000, 8, 16);

        // Act & Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, tooEarly),
                () -> assertThrows(IllegalArgumentException.class, tooLate));
    }

    @Test
    void of_monthOutsideYear_throwsIllegalArgument() {
        // Arrange
        Executable zero = () -> ForecastDate.of(2026, 0, 16);
        Executable thirteen = () -> ForecastDate.of(2026, 13, 16);

        // Act & Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, zero),
                () -> assertThrows(IllegalArgumentException.class, thirteen));
    }

    @Test
    void of_dayOutsideMonth_throwsIllegalArgument() {
        // Arrange
        Executable zero = () -> ForecastDate.of(2026, 8, 0);
        Executable thirtyTwo = () -> ForecastDate.of(2026, 8, 32);

        // Act & Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, zero),
                () -> assertThrows(IllegalArgumentException.class, thirtyTwo));
    }
}

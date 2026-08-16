package com.example.rxjava.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class TemperatureRangeTest {

    @Test
    void of_maxAndMin_keepsBothEnds() {
        // Arrange
        Temperature max = Temperature.ofCelsius(26);
        Temperature min = Temperature.ofCelsius(25);

        // Act
        TemperatureRange range = TemperatureRange.of(max, min);

        // Assert
        assertAll(
                () -> assertEquals(26, range.getMax().getCelsius()),
                () -> assertEquals(25, range.getMin().getCelsius()));
    }

    @Test
    void of_equalMaxAndMin_isAccepted() {
        // Arrange
        Temperature same = Temperature.ofCelsius(20);

        // Act
        TemperatureRange range = TemperatureRange.of(same, same);

        // Assert
        assertEquals(20, range.getMax().getCelsius());
    }

    @Test
    void of_maxColderThanMin_throwsIllegalArgument() {
        // Arrange
        Executable creation =
                () -> TemperatureRange.of(Temperature.ofCelsius(10), Temperature.ofCelsius(20));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, creation);
    }

    @Test
    void of_null_throwsNullPointer() {
        // Arrange
        Temperature value = Temperature.ofCelsius(20);

        // Act & Assert
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> TemperatureRange.of(null, value)),
                () -> assertThrows(NullPointerException.class,
                        () -> TemperatureRange.of(value, null)));
    }
}

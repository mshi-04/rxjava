package com.example.rxjava.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class TemperatureTest {

    @Test
    void ofCelsius_validValue_keepsCelsius() {
        // Arrange
        int celsius = 26;

        // Act
        Temperature temperature = Temperature.ofCelsius(celsius);

        // Assert
        assertEquals(26, temperature.getCelsius());
    }

    @Test
    void ofCelsius_bothBounds_areAccepted() {
        // Act
        Temperature lowest = Temperature.ofCelsius(-100);
        Temperature highest = Temperature.ofCelsius(100);

        // Assert
        assertAll(
                () -> assertEquals(-100, lowest.getCelsius()),
                () -> assertEquals(100, highest.getCelsius()));
    }

    @Test
    void ofCelsius_belowLowerBound_throwsIllegalArgument() {
        // Arrange
        Executable creation = () -> Temperature.ofCelsius(-101);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, creation);
    }

    @Test
    void ofCelsius_aboveUpperBound_throwsIllegalArgument() {
        // Arrange
        Executable creation = () -> Temperature.ofCelsius(101);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, creation);
    }

    @Test
    void isColderThan_differentValues_comparesByCelsius() {
        // Arrange
        Temperature cold = Temperature.ofCelsius(5);
        Temperature warm = Temperature.ofCelsius(25);

        // Act & Assert
        assertAll(
                () -> assertTrue(cold.isColderThan(warm)),
                () -> assertFalse(warm.isColderThan(cold)));
    }

    @Test
    void isColderThan_sameValue_returnsFalse() {
        // Arrange
        Temperature one = Temperature.ofCelsius(20);
        Temperature other = Temperature.ofCelsius(20);

        // Act
        boolean colder = one.isColderThan(other);

        // Assert
        assertFalse(colder);
    }
}

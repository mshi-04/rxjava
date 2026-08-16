package com.example.rxjava.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class WindSpeedTest {

    @Test
    void ofLevel_validLevel_keepsLevel() {
        // Act
        WindSpeed wind = WindSpeed.ofLevel(3);

        // Assert
        assertEquals(3, wind.getLevel());
    }

    @Test
    void ofLevel_bothBounds_areAccepted() {
        // Act
        WindSpeed calm = WindSpeed.ofLevel(0);
        WindSpeed strongest = WindSpeed.ofLevel(12);

        // Assert
        assertAll(
                () -> assertEquals(0, calm.getLevel()),
                () -> assertEquals(12, strongest.getLevel()));
    }

    @Test
    void ofLevel_negativeLevel_throwsIllegalArgument() {
        // Arrange
        Executable creation = () -> WindSpeed.ofLevel(-1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, creation);
    }

    @Test
    void ofLevel_aboveUpperBound_throwsIllegalArgument() {
        // Arrange
        Executable creation = () -> WindSpeed.ofLevel(13);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, creation);
    }
}

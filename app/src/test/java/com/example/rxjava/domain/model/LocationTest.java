package com.example.rxjava.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class LocationTest {

    @Test
    void of_validCoordinates_keepsLatitudeAndLongitude() {
        // Act
        Location location = Location.of(35.690, 139.692);

        // Assert
        assertAll(
                () -> assertEquals(35.690, location.getLatitude()),
                () -> assertEquals(139.692, location.getLongitude()));
    }

    @Test
    void TOKYO_holdsTokyoCoordinates() {
        // Act
        Location tokyo = Location.TOKYO;

        // Assert
        assertAll(
                () -> assertEquals(35.690, tokyo.getLatitude()),
                () -> assertEquals(139.692, tokyo.getLongitude()));
    }

    @Test
    void of_latitudeOutsideRange_throwsIllegalArgument() {
        // Arrange
        Executable tooFarSouth = () -> Location.of(-90.1, 0);
        Executable tooFarNorth = () -> Location.of(90.1, 0);

        // Act & Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, tooFarSouth),
                () -> assertThrows(IllegalArgumentException.class, tooFarNorth));
    }

    @Test
    void of_longitudeOutsideRange_throwsIllegalArgument() {
        // Arrange
        Executable tooFarWest = () -> Location.of(0, -180.1);
        Executable tooFarEast = () -> Location.of(0, 180.1);

        // Act & Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, tooFarWest),
                () -> assertThrows(IllegalArgumentException.class, tooFarEast));
    }
}

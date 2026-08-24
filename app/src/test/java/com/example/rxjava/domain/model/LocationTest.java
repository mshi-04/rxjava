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
    void of_boundaryCoordinates_acceptsExactLimits() {
        // Act
        Location southwest = Location.of(-90, -180);
        Location northeast = Location.of(90, 180);

        // Assert
        assertAll(
                () -> assertEquals(-90, southwest.getLatitude()),
                () -> assertEquals(-180, southwest.getLongitude()),
                () -> assertEquals(90, northeast.getLatitude()),
                () -> assertEquals(180, northeast.getLongitude()));
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

    @Test
    void of_nonFiniteCoordinates_throwsIllegalArgument() {
        // Arrange
        Executable nanLatitude = () -> Location.of(Double.NaN, 0);
        Executable infiniteLatitude = () -> Location.of(Double.POSITIVE_INFINITY, 0);
        Executable nanLongitude = () -> Location.of(0, Double.NaN);
        Executable infiniteLongitude = () -> Location.of(0, Double.NEGATIVE_INFINITY);

        // Act & Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, nanLatitude),
                () -> assertThrows(IllegalArgumentException.class, infiniteLatitude),
                () -> assertThrows(IllegalArgumentException.class, nanLongitude),
                () -> assertThrows(IllegalArgumentException.class, infiniteLongitude));
    }
}

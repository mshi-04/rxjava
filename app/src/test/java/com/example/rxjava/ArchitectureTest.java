package com.example.rxjava;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;

class ArchitectureTest {

    // テストクラスは本番と同じ package にあり mockwebserver3 や Retrofit を参照するため、
    // unit test の出力先（debugUnitTest 配下）から来たクラスは読み込まない。
    private static final ImportOption EXCLUDE_UNIT_TESTS =
            location -> !location.contains("UnitTest");

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(EXCLUDE_UNIT_TESTS)
            .importPackages("com.example.rxjava");

    @Test
    void domain_doesNotDependOnOuterLayersOrFrameworks() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAPackage("com.example.rxjava.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "io.reactivex.rxjava3.android..",
                        "retrofit2..",
                        "com.squareup.moshi..",
                        "okhttp3..",
                        "com.example.rxjava.data..",
                        "com.example.rxjava.presentation..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void data_doesNotDependOnAndroidOrPresentation() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAPackage("com.example.rxjava.data..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "android..",
                        "io.reactivex.rxjava3.android..",
                        "com.example.rxjava.presentation..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void dataOutsideDbAndDatasource_doesNotDependOnAndroidX() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAPackage("com.example.rxjava.data..")
                .and().resideOutsideOfPackages(
                        "com.example.rxjava.data.db..",
                        "com.example.rxjava.data.datasource..")
                .should().dependOnClassesThat().resideInAPackage("androidx..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void dbAndDatasource_doNotDependOnUnrelatedAndroidX() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        "com.example.rxjava.data.db..",
                        "com.example.rxjava.data.datasource..")
                .should().dependOnClassesThat(resideInAPackage("androidx..")
                        .and(not(resideInAnyPackage(
                                "androidx.room..", "androidx.sqlite..", "androidx.annotation.."))));

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void presentation_doesNotDependOnDataOrNetworkLibraries() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAPackage("com.example.rxjava.presentation..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.example.rxjava.data..",
                        "retrofit2..",
                        "com.squareup.moshi..",
                        "okhttp3..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void retrofitAndMoshi_outsideDataRemoteAndDi_areNotReferenced() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        "com.example.rxjava.domain..",
                        "com.example.rxjava.data..",
                        "com.example.rxjava.presentation..")
                .and().resideOutsideOfPackage("com.example.rxjava.data.remote..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "retrofit2..",
                        "com.squareup.moshi..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void remoteTypes_outsideRemoteUtilDatasourceAndDi_areNotReferenced() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        "com.example.rxjava.domain..",
                        "com.example.rxjava.data..",
                        "com.example.rxjava.presentation..")
                .and().resideOutsideOfPackages(
                        "com.example.rxjava.data.remote..",
                        "com.example.rxjava.data.util..",
                        "com.example.rxjava.data.datasource..")
                .should().dependOnClassesThat().resideInAPackage("com.example.rxjava.data.remote..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void dbTypes_outsideDbDatasourceAndDi_areNotReferenced() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        "com.example.rxjava.domain..",
                        "com.example.rxjava.data..",
                        "com.example.rxjava.presentation..")
                .and().resideOutsideOfPackages(
                        "com.example.rxjava.data.db..",
                        "com.example.rxjava.data.datasource..")
                .should().dependOnClassesThat().resideInAPackage("com.example.rxjava.data.db..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void utilTypes_outsideUtilAndDatasource_areNotReferenced() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAPackage("com.example.rxjava.data..")
                .and().resideOutsideOfPackages(
                        "com.example.rxjava.data.util..",
                        "com.example.rxjava.data.datasource..")
                .should().dependOnClassesThat().resideInAPackage("com.example.rxjava.data.util..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void datasourceTypes_outsideDatasourceAndRepository_areNotReferenced() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAPackage("com.example.rxjava.data..")
                .and().resideOutsideOfPackages(
                        "com.example.rxjava.data.datasource..",
                        "com.example.rxjava.data.repository..")
                .should().dependOnClassesThat().resideInAPackage("com.example.rxjava.data.datasource..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void repositoryTypes_outsideRepository_areNotReferenced() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAPackage("com.example.rxjava.data..")
                .and().resideOutsideOfPackages(
                        "com.example.rxjava.data.repository..")
                .should().dependOnClassesThat().resideInAPackage("com.example.rxjava.data.repository..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void domainAndData_declareNoHiltModules() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        "com.example.rxjava.domain..",
                        "com.example.rxjava.data..")
                .should().beAnnotatedWith(Module.class)
                .orShould().beAnnotatedWith(InstallIn.class);

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void domainAndData_declareNoProvidesOrBindsMethods() {
        // Arrange
        ArchRule rule = noMethods().that().areDeclaredInClassesThat().resideInAnyPackage(
                        "com.example.rxjava.domain..",
                        "com.example.rxjava.data..")
                .should().beAnnotatedWith(Provides.class)
                .orShould().beAnnotatedWith(Binds.class);

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }

    @Test
    void androidSchedulers_outsidePresentation_areNotReferenced() {
        // Arrange
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        "com.example.rxjava.domain..",
                        "com.example.rxjava.data..",
                        "com.example.rxjava.di..")
                .should().dependOnClassesThat().resideInAPackage("io.reactivex.rxjava3.android..");

        // Act
        Executable check = () -> rule.check(PRODUCTION_CLASSES);

        // Assert
        assertDoesNotThrow(check);
    }
}

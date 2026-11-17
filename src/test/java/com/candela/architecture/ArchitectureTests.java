package com.candela.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTests {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.candela");
    }

    @Test
    @DisplayName("No domain may directly access another domain's repositories")
    void noCrossDomainRepositoryAccess() {
        String[] domains = {"auth", "product", "inventory", "media", "content", "enquiry"};

        for (String sourceDomain : domains) {
            for (String targetDomain : domains) {
                if (!sourceDomain.equals(targetDomain)) {
                    noClasses()
                            .that().resideInAPackage("com.candela." + sourceDomain + "..")
                            .should().dependOnClassesThat().resideInAPackage("com.candela." + targetDomain + ".repository..")
                            .because("Cross-domain direct repository access violates modular monolith boundaries. Domain "
                                    + sourceDomain + " must not access " + targetDomain + " repository directly.")
                            .check(classes);
                }
            }
        }
    }

    @Test
    @DisplayName("Repositories must only be accessed by services within the same domain or security config")
    void repositoriesOnlyAccessedByDomainServices() {
        classes()
                .that().resideInAPackage("..repository..")
                .should().onlyBeAccessed().byAnyPackage(
                        "..service..",
                        "..repository..",
                        "..security..",
                        "..config..",
                        "com.candela"
                )
                .check(classes);
    }
}

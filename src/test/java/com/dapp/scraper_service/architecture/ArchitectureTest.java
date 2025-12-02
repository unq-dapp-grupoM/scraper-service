package com.dapp.scraper_service.architecture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

@AnalyzeClasses(packages = "com.dapp.scraper_service", importOptions = { ImportOption.DoNotIncludeTests.class })
public class ArchitectureTest {

    private static final String CONTROLLERS_PACKAGE = "..controller..";
    private static final String SERVICES_PACKAGE = "..service..";
    private static final String REPOSITORIES_PACKAGE = "..repository..";
    private static final String MODEL_PACKAGE = "..model..";
    private static final String AUDIT_PACKAGE = "..audit..";
    private static final String ENDPOINT_PACKAGE = "..endpoint..";
    private static final String CONFIG_PACKAGE = "..config..";

    @ArchTest
    public static final ArchRule layeredArchitectureRule =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Controllers").definedBy(CONTROLLERS_PACKAGE, ENDPOINT_PACKAGE)
                    .layer("Services").definedBy(SERVICES_PACKAGE)
                    .layer("Repositories").definedBy(REPOSITORIES_PACKAGE)
                    .layer("Aspects").definedBy(AUDIT_PACKAGE)

                    .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers", "Aspects")
                    .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services");

    // ### Naming and Annotation Rules ###

    @ArchTest
    public static final ArchRule controllerNamingAndAnnotationRule =
            classes()
                    .that().resideInAPackage(CONTROLLERS_PACKAGE)
                    .should().haveSimpleNameEndingWith("Controller")
                    .andShould().beAnnotatedWith(RestController.class)
                    .as("Controllers should end with 'Controller' and be annotated with @RestController");

    @ArchTest
    public static final ArchRule serviceNamingAndAnnotationRule =
            classes()
                    .that().resideInAPackage(SERVICES_PACKAGE)
                    .and().areNotAnonymousClasses()
                    .should().haveSimpleNameEndingWith("Service")
                    .andShould().beAnnotatedWith(Service.class)
                    .as("Services should end with 'Service' and be annotated with @Service");

    @ArchTest
    public static final ArchRule repositoryNamingAndAnnotationRule =
            classes()
                    .that().resideInAPackage(REPOSITORIES_PACKAGE)
                    .should().haveSimpleNameEndingWith("Repository")
                    .andShould().beInterfaces() // Spring Data JPA repositories are interfaces
                    .as("Repositories should end with 'Repository' and be interfaces");

    // ### Layer Access Rules ###

    @ArchTest
    public static final ArchRule servicesShouldOnlyBeAccessedByAllowedPackages =
            classes()
                    .that().resideInAPackage(SERVICES_PACKAGE)
                    .should().onlyBeAccessed().byAnyPackage(CONTROLLERS_PACKAGE, SERVICES_PACKAGE, AUDIT_PACKAGE, ENDPOINT_PACKAGE)
                    .as("Services should only be accessed by controllers, endpoints, aspects, or other services");

    @ArchTest
    public static final ArchRule repositoriesShouldOnlyBeAccessedByServices =
            classes()
                    .that().resideInAPackage(REPOSITORIES_PACKAGE)
                    .should().onlyBeAccessed().byClassesThat().resideInAPackage(SERVICES_PACKAGE)
                    .as("Repositories should only be accessed by services");

    @ArchTest
    public static final ArchRule domainModelsShouldNotDependOnOtherLayers =
            noClasses().that().resideInAPackage(MODEL_PACKAGE)
                    .should().dependOnClassesThat().resideInAnyPackage(CONTROLLERS_PACKAGE, SERVICES_PACKAGE, REPOSITORIES_PACKAGE, AUDIT_PACKAGE, ENDPOINT_PACKAGE);

    // ### General Coding Rules ###

    @ArchTest
    public static final ArchRule noFieldInjection =
            noFields()
                    .should().beAnnotatedWith(Autowired.class)
                    .as("Field injection is discouraged, use constructor injection instead");

    @ArchTest
    public static final ArchRule useSlf4jForLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
}
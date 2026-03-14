package dk.jnie.example.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dk.jnie.example..", importOptions = ImportOption.DoNotIncludeTests.class)
public class ModuleDependencyRules {

    @ArchTest
    public static final ArchRule domainModuleShouldNotDependOnOtherModules =
        noClasses()
            .that(resideInAPackage("dk.jnie.example.domain.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.service..",
                "dk.jnie.example.rest..",
                "dk.jnie.example.inbound..",
                "dk.jnie.example.outbound..",
                "dk.jnie.example.advice..",
                "dk.jnie.example.repository..",
                "jakarta.enterprise..",
                "io.quarkus.."
            ))
            .because("Domain module should not depend on any framework or other modules");

    @ArchTest
    public static final ArchRule serviceModuleShouldOnlyDependOnDomain =
        noClasses()
            .that(resideInAPackage("dk.jnie.example.service.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.rest..",
                "dk.jnie.example.inbound..",
                "dk.jnie.example.outbound..",
                "dk.jnie.example.advice..",
                "io.quarkus.."
            ))
            .because("Service module should only depend on domain (CDI annotations are OK)");

    @ArchTest
    public static final ArchRule inboundModuleShouldOnlyDependOnDomain =
        noClasses()
            .that(resideInAnyPackage("dk.jnie.example.rest..", "dk.jnie.example.inbound.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.service..",
                "dk.jnie.example.outbound..",
                "dk.jnie.example.advice.."
            ))
            .because("Inbound/REST module should only depend on domain");

    @ArchTest
    public static final ArchRule outboundModuleShouldOnlyDependOnDomain =
        noClasses()
            .that(resideInAnyPackage("dk.jnie.example.outbound..", "dk.jnie.example.advice.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.service..",
                "dk.jnie.example.rest..",
                "dk.jnie.example.inbound.."
            ))
            .because("Outbound module should only depend on domain");

    @ArchTest
    public static final ArchRule repositoryModuleShouldOnlyDependOnDomain =
        noClasses()
            .that(resideInAPackage("dk.jnie.example.repository.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.service..",
                "dk.jnie.example.rest..",
                "dk.jnie.example.inbound..",
                "dk.jnie.example.outbound..",
                "dk.jnie.example.advice.."
            ))
            .because("Repository module should only depend on domain");
}
package br.ufla.cooperativa;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Regras de arquitetura do Design (A1, A2, A3, A5, A6). A4 não é aplicada: ver README. */
class ArquiteturaTest {
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages("br.ufla.cooperativa");
    }

    @Test
    void a1_webNaoDependeDePersistence() {
        noClasses().that().resideInAPackage("..web..")
                .should().dependOnClassesThat().resideInAPackage("..persistence..").check(classes);
    }

    @Test
    void a2_webEPersistenceNaoAcessamRelogio() {
        noClasses().that().resideInAnyPackage("..web..", "..persistence..")
                .should().dependOnClassesThat().haveFullyQualifiedName("java.time.Clock")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("br.ufla.cooperativa.shared.TempoNegocio").check(classes);
    }

    @Test
    void a3_semCiclosEntreModulos() {
        slices().matching("br.ufla.cooperativa.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    void a5_proibidoRelogioSemClock() {
        noClasses().should().callMethod(Instant.class, "now")
                .orShould().callMethod(LocalDate.class, "now")
                .orShould().callMethod(LocalDateTime.class, "now")
                .orShould().callMethod(System.class, "currentTimeMillis").check(classes);
    }

    @Test
    void a6_controladoresNaoUsamEntidades() {
        noClasses().that().resideInAPackage("..web..")
                .should().dependOnClassesThat().areAnnotatedWith(jakarta.persistence.Entity.class).check(classes);
    }
}

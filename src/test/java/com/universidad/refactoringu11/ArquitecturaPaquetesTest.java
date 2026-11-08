package com.universidad.refactoringu11;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Prueba de arquitectura que verifica la ausencia de dependencias cíclicas
 * entre los paquetes del proyecto (Parte 2, Paso 2).
 *
 * Esta prueba FALLA en el commit 7 (con el ciclo inventario ↔ reportes)
 * y PASA en el commit 8 (tras aplicar Inversión de Dependencias).
 */
class ArquitecturaPaquetesTest {

    @Test
    void losPaquetesNoDebenTenerDependenciasCiclicas() {
        JavaClasses clases = new ClassFileImporter()
                .importPackages("com.universidad.refactoringu11");

        slices().matching("com.universidad.refactoringu11.(*)..")
                .should().beFreeOfCycles()
                .check(clases);
    }
}

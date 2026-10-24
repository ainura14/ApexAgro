package com.apexagro.architecture;


import com.apexagro.AgroBackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * BE3-02: проверка модульной структуры Spring Modulith.
 *
 * <p>{@code verify()} падает, если:
 * <ul>
 *   <li>между модулями есть циклическая зависимость;</li>
 *   <li>модуль обращается к внутренним пакетам другого модуля (всё, что ниже корневого пакета модуля
 *       и не объявлено через {@code @NamedInterface});</li>
 *   <li>модуль зависит от модуля, которого нет в его {@code allowedDependencies} (если список задан).</li>
 * </ul>
 */
class ModularityTests {

    private static final ApplicationModules MODULES = ApplicationModules.of(AgroBackendApplication.class);

    @Test
    void verifiesModularStructure() {
        MODULES.verify();
    }
    /** Диаграммы модулей для ревью и ADR: build/spring-modulith-docs. */
    @Test
    void writesModuleDocumentation() {
        new Documenter(MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}

package com.apexagro.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.apexagro.AgroBackendApplication;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * BE3-02: ArchUnit-правила границ модулей (дополняют {@link ModularityTests}).
 *
 * <p>Модуль = первый пакет под {@code com.apexagro}: {@code com.apexagro.delivery.internal.X} → модуль {@code delivery}.
 * Публичный API модуля (интерфейс {@code XxxApi}, DTO, события) — в корневом пакете модуля,
 * реализация — в {@code com.apexagro.<module>.internal..}.
 *
 * <p>{@code allowEmptyShould(true)}: в S0 модулей ещё почти нет, правило не должно падать от того,
 * что ему пока нечего проверять.
 */
@AnalyzeClasses(packagesOf = AgroBackendApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTests {

    private static final String ROOT = AgroBackendApplication.class.getPackageName();

    @ArchTest
    static final ArchRule internalPackagesAreModulePrivate = classes()
            .that().resideInAPackage(ROOT + ".*.internal..")
            .should(onlyBeAccessedFromOwnModule())
            .because("пакет internal — реализация модуля; другие модули ходят через его публичный XxxApi")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositoriesAreModulePrivate = classes()
            .that().areAssignableTo("org.springframework.data.repository.Repository")
            .should(onlyBeAccessedFromOwnModule())
            .because("таблицы модуля читает и пишет только сам модуль (нет репозиториев чужих модулей)")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositoriesLiveInInternal = classes()
            .that().areAssignableTo("org.springframework.data.repository.Repository")
            .should().resideInAPackage(ROOT + ".*.internal..")
            .because("репозиторий — деталь реализации модуля")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesAreModulePrivate = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .or().areAnnotatedWith("org.springframework.data.relational.core.mapping.Table")
            .should(onlyBeAccessedFromOwnModule())
            .because("сущность — модель хранения модуля; наружу отдаются DTO")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule commonDoesNotDependOnBusinessModules = noClasses()
            .that().resideInAPackage(ROOT + ".common..")
            .should().dependOnClassesThat(resideInAPackage(ROOT + "..")
                    .and(not(resideInAPackage(ROOT + ".common.."))))
            .because("common — общий каркас; бизнес-модули зависят от него, а не наоборот")
            .allowEmptyShould(true);

    /** BE3-15: любая задача по расписанию защищена ShedLock, иначе на двух экземплярах она выполнится дважды. */
    @ArchTest
    static final ArchRule scheduledTasksAreLocked = methods()
            .that().areAnnotatedWith("org.springframework.scheduling.annotation.Scheduled")
            .should().beAnnotatedWith("net.javacrumbs.shedlock.spring.annotation.SchedulerLock")
            .because("задача должна выполняться одним экземпляром (BE3-15)")
            .allowEmptyShould(true);

    static ArchCondition<JavaClass> onlyBeAccessedFromOwnModule() {
        return new ArchCondition<>("be accessed only from its own module") {
            @Override
            public void check(JavaClass target, ConditionEvents events) {
                String targetModule = moduleOf(target);
                for (Dependency dependency : target.getDirectDependenciesToSelf()) {
                    String originModule = moduleOf(dependency.getOriginClass());
                    if (originModule != null && !originModule.equals(targetModule)) {
                        events.add(SimpleConditionEvent.violated(dependency,
                                "модуль '" + originModule + "' обращается к закрытому классу модуля '"
                                        + targetModule + "': " + dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** Имя модуля по пакету или null, если класс вне модулей (корневой пакет, сторонние библиотеки). */
    static String moduleOf(JavaClass javaClass) {
        String pkg = javaClass.getPackageName();
        String prefix = ROOT + ".";
        if (!pkg.startsWith(prefix)) {
            return null;
        }
        String rest = pkg.substring(prefix.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
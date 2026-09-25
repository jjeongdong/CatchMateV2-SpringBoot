package com.back.catchmate.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String ROOT = "com.back.catchmate";

    @Test
    @DisplayName("전환된 BC 는 아키텍처 규칙을 지킨다")
    void migratedContextsFollowArchitectureRules() {
        // given
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);

        // when & then
        for (ArchRule rule :
                ArchitectureRules.all(ROOT, MigratedContexts.NAMES, MigratedContexts.CROSS_CONTEXT_ALLOWLIST)) {
            rule.check(classes);
        }
    }
}

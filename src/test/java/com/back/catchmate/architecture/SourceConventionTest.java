package com.back.catchmate.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SourceConventionTest {

    private static final Path MAIN_ROOT = Path.of("src/main/java/com/back/catchmate");

    @Test
    @DisplayName("전환된 BC 의 소스는 텍스트 규칙을 지킨다")
    void migratedContextSourcesFollowConventions() throws IOException {
        // given
        List<String> report = new ArrayList<>();

        // when
        for (String context : MigratedContexts.NAMES) {
            try (Stream<Path> paths = Files.walk(MAIN_ROOT.resolve(context))) {
                List<Path> javaFiles =
                        paths.filter(path -> path.toString().endsWith(".java")).toList();
                for (Path file : javaFiles) {
                    for (SourceConventionRules.Violation violation :
                            SourceConventionRules.check(Files.readString(file))) {
                        report.add(file + ":" + violation.lineNumber() + " [" + violation.rule() + "] "
                                + violation.line());
                    }
                }
            }
        }

        // then
        assertThat(report).isEmpty();
    }

    @Test
    @DisplayName("전환 목록의 이름은 실제 BC 패키지여야 한다")
    void migratedContextNamesExist() {
        assertThat(MigratedContexts.NAMES).allSatisfy(name -> {
            assertThat(name).isNotEqualTo("global");
            assertThat(MAIN_ROOT.resolve(name)).isDirectory();
        });
    }
}

package com.pipker.starter.file;

import com.pipker.starter.file.service.FileStorageException;
import com.pipker.starter.file.service.LocalFileStorageService;
import com.pipker.starter.file.service.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesReadsAndDeletesGeneratedFilesWithoutLeakingThePhysicalRoot() throws IOException {
        Path storageRoot = temporaryDirectory.resolve("files");
        LocalFileStorageService service = new LocalFileStorageService(storageRoot, "/files");

        assertThat(storageRoot).doesNotExist();

        StoredFile storedFile = service.store("Pipker file content".getBytes(StandardCharsets.UTF_8), "annual-report.PDF");

        assertThat(storageRoot).isDirectory();
        assertThat(storedFile.storageKey())
                .matches("\\d{4}/\\d{2}/\\d{2}/[A-Za-z0-9]{32}\\.pdf");
        assertThat(storedFile.accessPath()).isEqualTo("/files/" + storedFile.storageKey());
        assertThat(storedFile.accessPath())
                .doesNotContain("://")
                .doesNotContain(storageRoot.toString());
        assertThat(storedFile.originalFilename()).isEqualTo("annual-report.PDF");
        assertThat(storedFile.size()).isEqualTo("Pipker file content".getBytes(StandardCharsets.UTF_8).length);
        assertThat(service.load(storedFile.storageKey()).getInputStream().readAllBytes())
                .isEqualTo("Pipker file content".getBytes(StandardCharsets.UTF_8));
        assertThat(service.exists(storedFile.storageKey())).isTrue();
        assertThat(service.delete(storedFile.storageKey())).isTrue();
        assertThat(service.exists(storedFile.storageKey())).isFalse();
        assertThat(service.delete(storedFile.storageKey())).isFalse();
    }

    @Test
    void supportsCustomAccessPrefixAndNormalizesOriginalFilename() {
        LocalFileStorageService service = new LocalFileStorageService(
                temporaryDirectory.resolve("files"),
                "/public/assets/"
        );

        StoredFile storedFile = service.store(new byte[]{1, 2, 3}, "nested\\diagram.SVG");

        assertThat(storedFile.accessPath()).isEqualTo("/public/assets/" + storedFile.storageKey());
        assertThat(storedFile.originalFilename()).isEqualTo("diagram.SVG");
        assertThat(storedFile.storageKey()).endsWith(".svg");
    }

    @Test
    void rejectsUnsafeStorageKeysForEveryLookupOperation() {
        LocalFileStorageService service = new LocalFileStorageService(temporaryDirectory.resolve("files"), "/files");

        for (String unsafeKey : List.of("", "/absolute.txt", "../outside.txt", "a/../outside.txt", "a\\outside.txt", "a//outside.txt")) {
            assertThatIllegalArgumentException().isThrownBy(() -> service.accessPath(unsafeKey));
            assertThatIllegalArgumentException().isThrownBy(() -> service.exists(unsafeKey));
            assertThatIllegalArgumentException().isThrownBy(() -> service.delete(unsafeKey));
            assertThatIllegalArgumentException().isThrownBy(() -> service.load(unsafeKey));
        }
    }

    @Test
    void failsClearlyWhenReadingAMissingFile() {
        LocalFileStorageService service = new LocalFileStorageService(temporaryDirectory.resolve("files"), "/files");

        assertThatThrownBy(() -> service.load("2026/09/04/missing.txt"))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("File does not exist.");
    }
}

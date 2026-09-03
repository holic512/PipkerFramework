package com.pipker.starter.file;

import com.pipker.starter.file.service.StoredFile;
import com.pipker.starter.file.util.FileStorageUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class FileStorageUtilsTests {

    @Test
    void normalizesSafeKeysAndRelativeAccessPaths() {
        assertThat(FileStorageUtils.normalizeStorageKey("2026/09/04/Example-1.PNG"))
                .isEqualTo("2026/09/04/Example-1.PNG");
        assertThat(FileStorageUtils.normalizeAccessPath("/public/files/"))
                .isEqualTo("/public/files");
        assertThat(FileStorageUtils.toAccessPath("/files", "2026/09/04/item.txt"))
                .isEqualTo("/files/2026/09/04/item.txt");
    }

    @Test
    void rejectsUnsafeKeysAndAccessPrefixes() {
        assertThatIllegalArgumentException().isThrownBy(() -> FileStorageUtils.normalizeStorageKey("../secret.txt"));
        assertThatIllegalArgumentException().isThrownBy(() -> FileStorageUtils.normalizeStorageKey("/secret.txt"));
        assertThatIllegalArgumentException().isThrownBy(() -> FileStorageUtils.normalizeStorageKey("files\\secret.txt"));
        assertThatIllegalArgumentException().isThrownBy(() -> FileStorageUtils.normalizeStorageKey("files//secret.txt"));
        assertThatIllegalArgumentException().isThrownBy(() -> FileStorageUtils.normalizeAccessPath("files"));
        assertThatIllegalArgumentException().isThrownBy(() -> FileStorageUtils.normalizeAccessPath("/files/**"));
        assertThatIllegalArgumentException().isThrownBy(() -> FileStorageUtils.normalizeAccessPath("/files/../secret"));
        assertThat(FileStorageUtils.isValidAccessPath("/files")).isTrue();
        assertThat(FileStorageUtils.isValidAccessPath("/files/**")).isFalse();
    }

    @Test
    void keepsOnlySafeFilenameMetadataAndExtensions() {
        assertThat(FileStorageUtils.normalizeOriginalFilename("C:\\temp\\report.PDF"))
                .isEqualTo("report.PDF");
        assertThat(FileStorageUtils.safeExtension("report.PDF")).isEqualTo(".pdf");
        assertThat(FileStorageUtils.safeExtension("archive.tar.gz")).isEqualTo(".gz");
        assertThat(FileStorageUtils.safeExtension(".env")).isEmpty();
        assertThat(FileStorageUtils.safeExtension("report.bad-extension!")).isEmpty();
    }

    @Test
    void storedFileContractRejectsAbsoluteUrlsAndMismatchedKeys() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new StoredFile("2026/09/04/item.txt", "https://files.example/item.txt", "item.txt", 1)
        );
        assertThatIllegalArgumentException().isThrownBy(
                () -> new StoredFile("2026/09/04/item.txt", "/files/other.txt", "item.txt", 1)
        );
    }
}

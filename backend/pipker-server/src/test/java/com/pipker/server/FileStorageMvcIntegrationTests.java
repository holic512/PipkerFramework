package com.pipker.server;

import com.pipker.server.config.LocalDataDirectoryInitializer;
import com.pipker.server.config.SqliteDatabaseDirectoryInitializer;
import com.pipker.starter.file.service.FileStorageService;
import com.pipker.starter.file.service.StoredFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration(initializers = {
        LocalDataDirectoryInitializer.class,
        SqliteDatabaseDirectoryInitializer.class
})
class FileStorageMvcIntegrationTests {

    private static final Path DATA_ROOT = createTemporaryDirectory();
    private static final Path FILE_ROOT = DATA_ROOT.resolve("file");
    private static final Path SQLITE_DATABASE = SqliteTestDatabase.create("pipker-file-mvc-");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileStorageService fileStorageService;

    @DynamicPropertySource
    static void fileStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("pipker.data.root", DATA_ROOT::toString);
        SqliteTestDatabase.register(registry, SQLITE_DATABASE);
    }

    @AfterAll
    static void cleanTemporaryDirectory() throws IOException {
        SqliteTestDatabase.delete(SQLITE_DATABASE);
        List<Path> paths;
        try (Stream<Path> pathStream = Files.walk(FILE_ROOT)) {
            paths = pathStream.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void publicFilePathServesStoredContentWithoutAuthenticationOrAHostName() throws Exception {
        byte[] expectedContent = "public file response".getBytes(StandardCharsets.UTF_8);
        StoredFile storedFile = fileStorageService.store(expectedContent, "readme.txt");

        assertThat(Files.isRegularFile(FILE_ROOT.resolve(storedFile.storageKey()))).isTrue();
        assertThat(storedFile.accessPath()).startsWith("/files/");
        assertThat(storedFile.accessPath())
                .doesNotContain("://")
                .doesNotContain(FILE_ROOT.toString());

        mockMvc.perform(get(storedFile.accessPath()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedContent));
        mockMvc.perform(head(storedFile.accessPath()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/files/2026/09/04/missing.txt"))
                .andExpect(status().isNotFound());
    }

    private static Path createTemporaryDirectory() {
        try {
            return Files.createTempDirectory("pipker-file-mvc-");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

package com.pipker.server;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class SqliteTestDatabase {

    private SqliteTestDatabase() {
    }

    static Path create(String prefix) {
        try {
            return Files.createTempFile(prefix, ".db").toAbsolutePath();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create temporary SQLite database", exception);
        }
    }

    static void register(DynamicPropertyRegistry registry, Path databaseFile) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + databaseFile);
        registry.add("spring.datasource.username", () -> "");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
    }

    static void delete(Path databaseFile) throws IOException {
        Files.deleteIfExists(databaseFile);
    }
}

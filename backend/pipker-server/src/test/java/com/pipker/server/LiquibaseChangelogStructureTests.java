/**
 * @file LiquibaseChangelogStructureTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 离线验证共享 Liquibase XML changelog 的三种数据库分支和生成 SQL。
 * @logic 使用 OfflineConnection 为 MySQL、PostgreSQL 和 SQLite 分别解析 changelog、执行 validate，并通过 Liquibase SQL 生成器将变更输出到内存。
 * @dependencies Liquibase、JUnit Jupiter、AssertJ
 * @index_tags server、test、liquibase、offline、mysql、postgresql、sqlite
 * @author holic512
 */
package com.pipker.server;

import liquibase.Liquibase;
import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.filter.DbmsChangeSetFilter;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class LiquibaseChangelogStructureTests {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";
    private static final List<String> TABLE_CHANGELOGS = List.of(
            "db/changelog/system/system-user.xml",
            "db/changelog/system/system-role.xml",
            "db/changelog/system/system-permission.xml",
            "db/changelog/system/system-menu.xml",
            "db/changelog/system/system-api-resource.xml",
            "db/changelog/system/system-user-role.xml",
            "db/changelog/system/system-role-permission.xml",
            "db/changelog/system/system-role-menu.xml"
    );

    @Test
    void sharedChangelogGeneratesFinalSchemaForEverySupportedDatabase(@TempDir Path temporaryDirectory) throws Exception {
        assertFinalSchema("mysql", false, temporaryDirectory);
        assertFinalSchema("postgresql", false, temporaryDirectory);
        assertFinalSchema("sqlite", true, temporaryDirectory);
    }

    private void assertFinalSchema(String databaseShortName, boolean sqlite, Path temporaryDirectory) throws Exception {
        String sql = generateSql(databaseShortName, temporaryDirectory);
        String normalizedSql = sql.toUpperCase(Locale.ROOT);

        assertThat(normalizedSql)
                .contains("CREATE TABLE SYSTEM_USER")
                .contains("CREATE TABLE SYSTEM_ROLE")
                .contains("CREATE TABLE SYSTEM_PERMISSION")
                .contains("CREATE TABLE SYSTEM_MENU")
                .contains("CREATE TABLE SYSTEM_API_RESOURCE")
                .contains("CREATE TABLE SYSTEM_USER_ROLE")
                .contains("CREATE TABLE SYSTEM_ROLE_PERMISSION")
                .contains("CREATE TABLE SYSTEM_ROLE_MENU")
                .contains("SYSTEM:AUTH:ME")
                .contains("SYSTEM:ROLE-MENU:MANAGE")
                .contains("SYSTEMOVERVIEW")
                .contains("CK_SYSTEM_PERMISSION_TYPE")
                .contains("CK_SYSTEM_MENU_TYPE");

        int menuDefinitionStart = normalizedSql.indexOf("CREATE TABLE SYSTEM_MENU");
        int apiResourceDefinitionStart = normalizedSql.indexOf("CREATE TABLE SYSTEM_API_RESOURCE");
        assertThat(normalizedSql.substring(menuDefinitionStart, apiResourceDefinitionStart))
                .doesNotContain("PERMISSION_CODE");

        if (sqlite) {
            assertThat(normalizedSql)
                    .contains("PRIMARY KEY AUTOINCREMENT")
                    .contains("FOREIGN KEY (PARENT_ID)")
                    .doesNotContain("ALTER TABLE SYSTEM_ROLE_MENU ADD CONSTRAINT");
            return;
        }

        assertThat(normalizedSql)
                .doesNotContain("AUTOINCREMENT")
                .contains("ALTER TABLE SYSTEM_ROLE_MENU ADD CONSTRAINT")
                .contains("FK_SYSTEM_ROLE_MENU_ROLE");
    }

    private String generateSql(String databaseShortName, Path temporaryDirectory) throws Exception {
        Path historyFile = temporaryDirectory.resolve(databaseShortName + "-databasechangelog.csv");
        try (ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor()) {
            validateMasterChangelog(databaseShortName, historyFile, resourceAccessor);

            StringWriter output = new StringWriter();
            for (String tableChangelog : TABLE_CHANGELOGS) {
                Database database = offlineDatabase(databaseShortName, historyFile, resourceAccessor);
                try (Liquibase liquibase = new Liquibase(tableChangelog, resourceAccessor, database)) {
                    liquibase.validate();
                    appendGeneratedSql(liquibase, database, output);
                } finally {
                    database.close();
                }
            }
            return output.toString();
        }
    }

    private void validateMasterChangelog(String databaseShortName, Path historyFile, ResourceAccessor resourceAccessor) throws Exception {
        Database database = offlineDatabase(databaseShortName, historyFile, resourceAccessor);
        try (Liquibase liquibase = new Liquibase(CHANGELOG, resourceAccessor, database)) {
            liquibase.validate();
        } finally {
            database.close();
        }
    }

    private void appendGeneratedSql(Liquibase liquibase, Database database, StringWriter output) throws Exception {
        DbmsChangeSetFilter dbmsFilter = new DbmsChangeSetFilter(database);
        for (ChangeSet changeSet : liquibase.getDatabaseChangeLog().getChangeSets()) {
            if (!dbmsFilter.accepts(changeSet).isAccepted()) {
                continue;
            }
            for (Change change : changeSet.getChanges()) {
                for (Sql sql : SqlGeneratorFactory.getInstance().generateSql(change, database)) {
                    output.append(sql.toSql()).append(';').append(System.lineSeparator());
                }
            }
        }
    }

    private Database offlineDatabase(String databaseShortName, Path historyFile, ResourceAccessor resourceAccessor) throws Exception {
        String encodedHistoryFile = URLEncoder.encode(historyFile.toString(), StandardCharsets.UTF_8);
        OfflineConnection connection = new OfflineConnection(
                "offline:" + databaseShortName + "?changeLogFile=" + encodedHistoryFile,
                resourceAccessor
        );
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
        return database;
    }
}

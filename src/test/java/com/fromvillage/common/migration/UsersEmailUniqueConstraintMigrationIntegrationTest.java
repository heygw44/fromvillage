package com.fromvillage.common.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class UsersEmailUniqueConstraintMigrationIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @BeforeEach
    void setUp() throws SQLException {
        execute("DROP PROCEDURE IF EXISTS normalize_users_email_unique_constraint");
        execute("DROP TABLE IF EXISTS flyway_schema_history");
        execute("DROP TABLE IF EXISTS users");
    }

    @Test
    @DisplayName("기존 유니크 인덱스 이름이 다르면 uk_users_email로 변경한다")
    void renamesLegacyUniqueIndexNameToCanonicalName() throws SQLException {
        execute("CREATE TABLE users (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(320) NOT NULL, " +
                "password VARCHAR(100) NOT NULL, " +
                "nickname VARCHAR(50) NOT NULL, " +
                "role VARCHAR(20) NOT NULL" +
                ")");
        execute("ALTER TABLE users ADD CONSTRAINT `users.email` UNIQUE (email)");

        migrate();

        assertThat(findEmailUniqueIndexNames())
                .containsExactly("uk_users_email");
    }

    @Test
    @DisplayName("이미 uk_users_email이면 no-op으로 통과한다")
    void keepsCanonicalConstraintNameAsIs() throws SQLException {
        execute("CREATE TABLE users (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(320) NOT NULL, " +
                "password VARCHAR(100) NOT NULL, " +
                "nickname VARCHAR(50) NOT NULL, " +
                "role VARCHAR(20) NOT NULL, " +
                "CONSTRAINT uk_users_email UNIQUE (email)" +
                ")");

        migrate();

        assertThat(findEmailUniqueIndexNames())
                .containsExactly("uk_users_email");
    }

    @Test
    @DisplayName("email 유니크 인덱스가 없으면 uk_users_email을 생성한다")
    void createsCanonicalUniqueIndexWhenMissing() throws SQLException {
        execute("CREATE TABLE users (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(320) NOT NULL, " +
                "password VARCHAR(100) NOT NULL, " +
                "nickname VARCHAR(50) NOT NULL, " +
                "role VARCHAR(20) NOT NULL" +
                ")");

        migrate();

        assertThat(findEmailUniqueIndexNames())
                .containsExactly("uk_users_email");
    }

    @Test
    @DisplayName("email 유니크 인덱스가 복수면 자동 수정하지 않고 실패한다")
    void failsFastWhenMultipleEmailUniqueIndexesExist() throws SQLException {
        execute("CREATE TABLE users (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(320) NOT NULL, " +
                "password VARCHAR(100) NOT NULL, " +
                "nickname VARCHAR(50) NOT NULL, " +
                "role VARCHAR(20) NOT NULL, " +
                "CONSTRAINT uk_users_email UNIQUE (email), " +
                "CONSTRAINT legacy_users_email_uk UNIQUE (email)" +
                ")");

        assertThatThrownBy(this::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("users.email에 유니크 인덱스가 복수로 존재합니다.");
    }

    private void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target("1")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        flyway.migrate();
    }

    private List<String> findEmailUniqueIndexNames() throws SQLException {
        String sql = "SELECT s.index_name " +
                "FROM information_schema.statistics s " +
                "WHERE s.table_schema = DATABASE() " +
                "  AND s.table_name = 'users' " +
                "  AND s.non_unique = 0 " +
                "GROUP BY s.index_name " +
                "HAVING SUM(CASE WHEN s.column_name = 'email' THEN 1 ELSE 0 END) = 1 " +
                "   AND COUNT(*) = 1 " +
                "ORDER BY s.index_name";

        List<String> indexNames = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                indexNames.add(resultSet.getString(1));
            }
        }
        return indexNames;
    }

    private void execute(String sql) throws SQLException {
        executeOnDatabase(sql);
    }

    private void executeOnDatabase(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String jdbcUrl() {
        return "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                .formatted(MYSQL.getHost(), MYSQL.getMappedPort(3306), MYSQL.getDatabaseName());
    }
}

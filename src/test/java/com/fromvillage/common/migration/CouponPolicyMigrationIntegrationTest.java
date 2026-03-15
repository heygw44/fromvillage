package com.fromvillage.common.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class CouponPolicyMigrationIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @BeforeEach
    void setUp() throws SQLException {
        execute("DROP TABLE IF EXISTS flyway_schema_history");
        execute("DROP TABLE IF EXISTS order_item");
        execute("DROP TABLE IF EXISTS seller_order");
        execute("DROP TABLE IF EXISTS checkout_order");
        execute("DROP TABLE IF EXISTS cart_item");
        execute("DROP TABLE IF EXISTS coupon_policy");
        execute("DROP TABLE IF EXISTS products");
        execute("DROP TABLE IF EXISTS users");

        execute("""
                CREATE TABLE users (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(320) NOT NULL,
                    password VARCHAR(100) NOT NULL,
                    nickname VARCHAR(50) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    seller_approved_at DATETIME(6) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    CONSTRAINT uk_users_email UNIQUE (email)
                )
                """);

        execute("""
                CREATE TABLE products (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    seller_id BIGINT NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    description TEXT NOT NULL,
                    category VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    price BIGINT NOT NULL,
                    stock_quantity INT NOT NULL,
                    image_url VARCHAR(2048) NOT NULL,
                    deleted_at DATETIME(6) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    CONSTRAINT fk_products_seller
                        FOREIGN KEY (seller_id) REFERENCES users (id),
                    CONSTRAINT ck_products_price
                        CHECK (price > 0),
                    CONSTRAINT ck_products_stock_quantity
                        CHECK (stock_quantity >= 0)
                )
                """);
    }

    @Test
    @DisplayName("coupon_policy 테이블을 생성한다")
    void createsCouponPolicyTable() throws SQLException {
        migrate();

        assertThat(tableExists("coupon_policy")).isTrue();
    }

    @Test
    @DisplayName("coupon_policy 테이블은 쿠폰 정책 운영에 필요한 컬럼을 모두 가진다")
    void createsCouponPolicyColumns() throws SQLException {
        migrate();

        Map<String, String> columns = findColumnTypes("coupon_policy");

        assertThat(columns).containsEntry("id", "BIGINT");
        assertThat(columns).containsEntry("name", "VARCHAR");
        assertThat(columns).containsEntry("discount_amount", "BIGINT");
        assertThat(columns).containsEntry("minimum_order_amount", "BIGINT");
        assertThat(columns).containsEntry("total_quantity", "INT");
        assertThat(columns).containsEntry("issued_quantity", "INT");
        assertThat(columns).containsEntry("started_at", "DATETIME");
        assertThat(columns).containsEntry("ended_at", "DATETIME");
        assertThat(columns).containsEntry("status", "VARCHAR");
        assertThat(columns).containsEntry("created_by", "BIGINT");
        assertThat(columns).containsEntry("created_at", "DATETIME");
        assertThat(columns).containsEntry("updated_at", "DATETIME");
    }

    @Test
    @DisplayName("coupon_policy.created_by 는 users.id 를 참조하는 외래 키를 가진다")
    void createsCouponPolicyCreatedByForeignKey() throws SQLException {
        migrate();

        assertThat(hasForeignKey("coupon_policy", "created_by", "users", "id")).isTrue();
    }

    @Test
    @DisplayName("coupon_policy 주요 컬럼은 NOT NULL 제약을 가진다")
    void createsCouponPolicyNotNullColumns() throws SQLException {
        migrate();

        assertThat(isNullable("coupon_policy", "name")).isFalse();
        assertThat(isNullable("coupon_policy", "discount_amount")).isFalse();
        assertThat(isNullable("coupon_policy", "minimum_order_amount")).isFalse();
        assertThat(isNullable("coupon_policy", "total_quantity")).isFalse();
        assertThat(isNullable("coupon_policy", "issued_quantity")).isFalse();
        assertThat(isNullable("coupon_policy", "started_at")).isFalse();
        assertThat(isNullable("coupon_policy", "ended_at")).isFalse();
        assertThat(isNullable("coupon_policy", "status")).isFalse();
        assertThat(isNullable("coupon_policy", "created_by")).isFalse();
        assertThat(isNullable("coupon_policy", "created_at")).isFalse();
        assertThat(isNullable("coupon_policy", "updated_at")).isFalse();
    }

    @Test
    @DisplayName("coupon_policy 는 금액과 수량에 대한 check 제약을 가진다")
    void createsCouponPolicyCheckConstraints() throws SQLException {
        migrate();

        assertThat(normalizeClause(findCheckClause("coupon_policy", "ck_coupon_policy_discount_amount")))
                .contains("discount_amount > 0");
        assertThat(normalizeClause(findCheckClause("coupon_policy", "ck_coupon_policy_minimum_order_amount")))
                .contains("minimum_order_amount >= 0");
        assertThat(normalizeClause(findCheckClause("coupon_policy", "ck_coupon_policy_total_quantity")))
                .contains("total_quantity >= 1");
        assertThat(normalizeClause(findCheckClause("coupon_policy", "ck_coupon_policy_issued_quantity")))
                .contains("issued_quantity >= 0");
    }

    private void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target("5")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        flyway.migrate();
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = connection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, tableName, null)) {
                return resultSet.next();
            }
        }
    }

    private Map<String, String> findColumnTypes(String tableName) throws SQLException {
        String sql = """
                SELECT column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                ORDER BY ordinal_position
                """;

        Map<String, String> columns = new HashMap<>();
        try (Connection connection = connection();
             var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, tableName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    columns.put(
                            resultSet.getString("column_name"),
                            resultSet.getString("data_type").toUpperCase()
                    );
                }
            }
        }
        return columns;
    }

    private boolean hasForeignKey(
            String tableName,
            String columnName,
            String referencedTable,
            String referencedColumn
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                  AND referenced_table_name = ?
                  AND referenced_column_name = ?
                """;

        try (Connection connection = connection();
             var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, columnName);
            preparedStatement.setString(3, referencedTable);
            preparedStatement.setString(4, referencedColumn);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private boolean isNullable(String tableName, String columnName) throws SQLException {
        String sql = """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """;

        try (Connection connection = connection();
             var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, columnName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                resultSet.next();
                return "YES".equals(resultSet.getString(1));
            }
        }
    }

    private String findCheckClause(String tableName, String constraintName) throws SQLException {
        String sql = """
                SELECT check_clause
                FROM information_schema.check_constraints check_constraints
                join information_schema.table_constraints table_constraints
                  on table_constraints.constraint_schema = check_constraints.constraint_schema
                 and table_constraints.constraint_name = check_constraints.constraint_name
                WHERE check_constraints.constraint_schema = DATABASE()
                  AND table_constraints.table_name = ?
                  AND check_constraints.constraint_name = ?
                """;

        try (Connection connection = connection();
             var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, constraintName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return "";
                }
                return resultSet.getString(1);
            }
        }
    }

    private String normalizeClause(String clause) {
        return clause == null ? "" : clause.replace("`", "");
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                jdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        );
    }

    private String jdbcUrl() {
        return "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                .formatted(MYSQL.getHost(), MYSQL.getMappedPort(3306), MYSQL.getDatabaseName());
    }
}

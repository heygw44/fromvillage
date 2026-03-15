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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class IssuedCouponMigrationIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @BeforeEach
    void setUp() throws SQLException {
        execute("DROP TABLE IF EXISTS flyway_schema_history");
        execute("DROP TABLE IF EXISTS issued_coupon");
        execute("DROP TABLE IF EXISTS coupon_policy");
        execute("DROP TABLE IF EXISTS order_item");
        execute("DROP TABLE IF EXISTS seller_order");
        execute("DROP TABLE IF EXISTS checkout_order");
        execute("DROP TABLE IF EXISTS cart_item");
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
    @DisplayName("issued_coupon 테이블을 생성한다")
    void createsIssuedCouponTable() throws SQLException {
        migrate();

        assertThat(tableExists("issued_coupon")).isTrue();
    }

    @Test
    @DisplayName("issued_coupon 테이블은 쿠폰 발급 이력에 필요한 컬럼을 모두 가진다")
    void createsIssuedCouponColumns() throws SQLException {
        migrate();

        Map<String, String> columns = findColumnTypes("issued_coupon");

        assertThat(columns).containsEntry("id", "BIGINT");
        assertThat(columns).containsEntry("coupon_policy_id", "BIGINT");
        assertThat(columns).containsEntry("user_id", "BIGINT");
        assertThat(columns).containsEntry("status", "VARCHAR");
        assertThat(columns).containsEntry("issued_at", "DATETIME");
        assertThat(columns).containsEntry("used_at", "DATETIME");
        assertThat(columns).containsEntry("created_at", "DATETIME");
        assertThat(columns).containsEntry("updated_at", "DATETIME");
    }

    @Test
    @DisplayName("issued_coupon 은 coupon_policy 와 users 를 참조하는 외래 키를 가진다")
    void createsIssuedCouponForeignKeys() throws SQLException {
        migrate();

        assertThat(hasForeignKey("issued_coupon", "coupon_policy_id", "coupon_policy", "id")).isTrue();
        assertThat(hasForeignKey("issued_coupon", "user_id", "users", "id")).isTrue();
    }

    @Test
    @DisplayName("issued_coupon 주요 컬럼은 NOT NULL 제약을 가진다")
    void createsIssuedCouponNotNullColumns() throws SQLException {
        migrate();

        assertThat(isNullable("issued_coupon", "coupon_policy_id")).isFalse();
        assertThat(isNullable("issued_coupon", "user_id")).isFalse();
        assertThat(isNullable("issued_coupon", "status")).isFalse();
        assertThat(isNullable("issued_coupon", "issued_at")).isFalse();
        assertThat(isNullable("issued_coupon", "created_at")).isFalse();
        assertThat(isNullable("issued_coupon", "updated_at")).isFalse();
    }

    @Test
    @DisplayName("issued_coupon 은 같은 사용자와 쿠폰 정책 조합에 대한 유니크 제약을 가진다")
    void createsIssuedCouponUniqueConstraint() throws SQLException {
        migrate();

        assertThat(hasUniqueConstraint("issued_coupon", "uk_issued_coupon_policy_user")).isTrue();
    }

    @Test
    @DisplayName("같은 사용자에 대한 동일 쿠폰 정책 발급 이력은 중복 저장할 수 없다")
    void rejectsDuplicateIssuedCouponForSamePolicyAndUser() throws SQLException {
        migrate();

        execute("""
                INSERT INTO users (email, password, nickname, role, seller_approved_at, created_at, updated_at)
                VALUES ('admin@test.com', 'pw', '운영자', 'ADMIN', NULL, NOW(6), NOW(6))
                """);

        execute("""
                INSERT INTO users (email, password, nickname, role, seller_approved_at, created_at, updated_at)
                VALUES ('user@test.com', 'pw', '일반회원', 'USER', NULL, NOW(6), NOW(6))
                """);

        execute("""
                INSERT INTO coupon_policy
                (name, discount_amount, minimum_order_amount, total_quantity, issued_quantity,
                 started_at, ended_at, status, created_by, created_at, updated_at)
                VALUES ('봄맞이 할인', 3000, 20000, 100, 0,
                        NOW(6), DATE_ADD(NOW(6), INTERVAL 1 DAY), 'OPEN', 1, NOW(6), NOW(6))
                """);

        execute("""
                INSERT INTO issued_coupon
                (coupon_policy_id, user_id, status, issued_at, used_at, created_at, updated_at)
                VALUES (1, 2, 'ISSUED', NOW(6), NULL, NOW(6), NOW(6))
                """);

        assertThatThrownBy(() -> execute("""
                INSERT INTO issued_coupon
                (coupon_policy_id, user_id, status, issued_at, used_at, created_at, updated_at)
                VALUES (1, 2, 'ISSUED', NOW(6), NULL, NOW(6), NOW(6))
                """))
                .isInstanceOf(SQLException.class);
    }

    private void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target("6")
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
            String referencedTableName,
            String referencedColumnName
    ) throws SQLException {
        try (Connection connection = connection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getImportedKeys(connection.getCatalog(), null, tableName)) {
                while (resultSet.next()) {
                    if (columnName.equalsIgnoreCase(resultSet.getString("FKCOLUMN_NAME"))
                            && referencedTableName.equalsIgnoreCase(resultSet.getString("PKTABLE_NAME"))
                            && referencedColumnName.equalsIgnoreCase(resultSet.getString("PKCOLUMN_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
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
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("컬럼을 찾을 수 없습니다. table=%s, column=%s"
                            .formatted(tableName, columnName));
                }
                return "YES".equalsIgnoreCase(resultSet.getString("is_nullable"));
            }
        }
    }

    private boolean hasUniqueConstraint(String tableName, String constraintName) throws SQLException {
        String sql = """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_type = 'UNIQUE'
                  AND constraint_name = ?
                """;

        try (Connection connection = connection();
             var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, constraintName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private String jdbcUrl() {
        return MYSQL.getJdbcUrl();
    }
}

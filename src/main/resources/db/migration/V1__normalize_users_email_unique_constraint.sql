DELIMITER $$

DROP PROCEDURE IF EXISTS normalize_users_email_unique_constraint$$

CREATE PROCEDURE normalize_users_email_unique_constraint()
BEGIN
    DECLARE existing_email_unique_index_name VARCHAR(64);
    DECLARE email_unique_index_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO email_unique_index_count
    FROM (
             SELECT s.index_name
             FROM information_schema.statistics s
             WHERE s.table_schema = DATABASE()
               AND s.table_name = 'users'
               AND s.non_unique = 0
             GROUP BY s.index_name
             HAVING SUM(CASE WHEN s.column_name = 'email' THEN 1 ELSE 0 END) = 1
                AND COUNT(*) = 1
         ) unique_email_indexes;

    IF email_unique_index_count > 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'users.email에 유니크 인덱스가 복수로 존재합니다. 운영 점검 후 수동 정리해 주세요.';
    END IF;

    SELECT unique_email_indexes.index_name
    INTO existing_email_unique_index_name
    FROM (
             SELECT s.index_name
             FROM information_schema.statistics s
             WHERE s.table_schema = DATABASE()
               AND s.table_name = 'users'
               AND s.non_unique = 0
             GROUP BY s.index_name
             HAVING SUM(CASE WHEN s.column_name = 'email' THEN 1 ELSE 0 END) = 1
                AND COUNT(*) = 1
         ) unique_email_indexes
    LIMIT 1;

    IF existing_email_unique_index_name IS NULL THEN
        SET @statement_sql = 'ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email)';
    ELSEIF existing_email_unique_index_name = 'uk_users_email' THEN
        SET @statement_sql = 'SELECT 1';
    ELSE
        SET @statement_sql = CONCAT(
                'ALTER TABLE users RENAME INDEX `',
                REPLACE(existing_email_unique_index_name, '`', '``'),
                '` TO `uk_users_email`'
                            );
    END IF;

    PREPARE statement_handle FROM @statement_sql;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
END$$

DELIMITER ;

CALL normalize_users_email_unique_constraint();
DROP PROCEDURE normalize_users_email_unique_constraint;

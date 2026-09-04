package com.fsm.keystone.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaRepairRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE work_orders DROP CONSTRAINT IF EXISTS work_orders_status_check");
        jdbcTemplate.execute("ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS sla_status varchar(32)");
        jdbcTemplate.execute("ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS sla_due_at timestamp");
        jdbcTemplate.execute("ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS total_parts_cost numeric(19,2)");
        jdbcTemplate.execute("ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS total_minutes integer");
        jdbcTemplate.execute("ALTER TABLE part_usage ADD COLUMN IF NOT EXISTS usage_status varchar(32)");
        jdbcTemplate.execute("UPDATE part_usage SET usage_status = 'USED' WHERE usage_status IS NULL");
        jdbcTemplate.execute("ALTER TABLE status_history DROP CONSTRAINT IF EXISTS status_history_old_status_check");
        jdbcTemplate.execute("ALTER TABLE status_history DROP CONSTRAINT IF EXISTS status_history_new_status_check");
        jdbcTemplate.execute("""
                UPDATE work_orders
                SET sla_status = status, status = 'NEW'
                WHERE status IN ('ON_TRACK', 'AT_RISK', 'BREACHED')
                """);
        jdbcTemplate.execute("""
                UPDATE work_orders
                SET sla_due_at = COALESCE(created_at, NOW()) +
                    CASE priority
                        WHEN 'CRITICAL' THEN INTERVAL '4 hours'
                        WHEN 'HIGH' THEN INTERVAL '8 hours'
                        WHEN 'LOW' THEN INTERVAL '72 hours'
                        ELSE INTERVAL '24 hours'
                    END
                WHERE sla_due_at IS NULL
                """);
        jdbcTemplate.execute("""
                UPDATE work_orders wo
                SET created_by_user_id = (
                    SELECT sh.changed_by_user_id
                    FROM status_history sh
                    WHERE sh.work_order_id = wo.id
                      AND sh.changed_by_user_id IS NOT NULL
                    ORDER BY sh.changed_at ASC
                    LIMIT 1
                )
                WHERE wo.created_by_user_id IS NULL
                """);
        jdbcTemplate.execute("""
                UPDATE users u
                SET customer_id = c.id
                FROM customers c
                WHERE u.role = 'CUSTOMER'
                  AND u.customer_id IS NULL
                  AND lower(trim(u.full_name)) = lower(trim(c.name))
                """);
        jdbcTemplate.execute("ALTER TABLE parts ADD COLUMN IF NOT EXISTS reorder_level integer");
        jdbcTemplate.execute("UPDATE parts SET reorder_level = 10 WHERE reorder_level IS NULL");
        jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        jdbcTemplate.execute("""
                ALTER TABLE users ADD CONSTRAINT users_role_check
                CHECK (role IN ('ADMIN','MANAGER','DISPATCHER','TECHNICIAN','CUSTOMER'))
                """);
    }
}

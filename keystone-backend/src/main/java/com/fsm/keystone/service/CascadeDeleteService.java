package com.fsm.keystone.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CascadeDeleteService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void deleteWorkOrder(Long id) {
        jdbcTemplate.update("DELETE FROM notifications WHERE work_order_id = ?", id);
        jdbcTemplate.update("DELETE FROM status_history WHERE work_order_id = ?", id);
        jdbcTemplate.update("DELETE FROM part_usage WHERE work_order_id = ?", id);
        jdbcTemplate.update("DELETE FROM time_logs WHERE work_order_id = ?", id);
        jdbcTemplate.update("DELETE FROM work_orders WHERE id = ?", id);
    }

    @Transactional
    public void deleteSite(Long id) {
        List<Long> workOrders = jdbcTemplate.queryForList(
                "SELECT id FROM work_orders WHERE site_id = ?", Long.class, id);
        workOrders.forEach(this::deleteWorkOrder);
        jdbcTemplate.update("DELETE FROM sites WHERE id = ?", id);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        jdbcTemplate.update("UPDATE users SET customer_id = NULL WHERE customer_id = ?", id);
        List<Long> sites = jdbcTemplate.queryForList(
                "SELECT id FROM sites WHERE customer_id = ?", Long.class, id);
        sites.forEach(this::deleteSite);
        List<Long> leftover = jdbcTemplate.queryForList(
                "SELECT id FROM work_orders WHERE customer_id = ?", Long.class, id);
        leftover.forEach(this::deleteWorkOrder);
        jdbcTemplate.update("DELETE FROM customers WHERE id = ?", id);
    }

    @Transactional
    public void deletePart(Long id) {
        jdbcTemplate.update("DELETE FROM part_usage WHERE part_id = ?", id);
        jdbcTemplate.update("DELETE FROM parts WHERE id = ?", id);
    }

    @Transactional
    public void deleteUser(Long id) {
        jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id);
        jdbcTemplate.update("UPDATE work_orders SET assigned_technician_id = NULL WHERE assigned_technician_id = ?", id);
        jdbcTemplate.update("UPDATE work_orders SET created_by_user_id = NULL WHERE created_by_user_id = ?", id);
        jdbcTemplate.update("UPDATE status_history SET changed_by_user_id = NULL WHERE changed_by_user_id = ?", id);
        jdbcTemplate.update("UPDATE part_usage SET used_by_user_id = NULL WHERE used_by_user_id = ?", id);
        jdbcTemplate.update("DELETE FROM time_logs WHERE technician_id = ?", id);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }
}

package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.enums.PaymentMethod;
import com.bazylev.server.models.entities.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO extends BaseDAO<Payment> {

    @Override
    protected String getTableName() {
        return "payment";
    }

    @Override
    protected Payment mapRow(ResultSet rs) throws SQLException {
        Date paymentDate = rs.getDate("payment_date");
        String method = rs.getString("payment_method");
        return new Payment(
                rs.getInt("id"),
                rs.getInt("student_id"),
                rs.getBigDecimal("amount"),
                paymentDate != null ? paymentDate.toLocalDate() : null,
                method != null ? PaymentMethod.valueOf(method) : null,
                rs.getString("receipt_number")
        );
    }

    public int save(Payment payment) {
        String sql = "INSERT INTO payment (student_id, amount, payment_date, payment_method, receipt_number) VALUES (?, ?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setInt(1, payment.getStudentId());
                stmt.setBigDecimal(2, payment.getAmount());
                stmt.setDate(3, payment.getPaymentDate() != null ? Date.valueOf(payment.getPaymentDate()) : null);
                stmt.setString(4, payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
                stmt.setString(5, payment.getReceiptNumber());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения платежа", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Payment> findByStudentId(int studentId) {
        String sql = "SELECT * FROM payment WHERE student_id = ? ORDER BY payment_date DESC";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            List<Payment> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения платежей студента", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public BigDecimal getTotalPaidByStudent(int studentId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE student_id = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения суммы платежей", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Payment> findByDateRange(LocalDate from, LocalDate to) {
        String sql = "SELECT * FROM payment WHERE payment_date BETWEEN ? AND ? ORDER BY payment_date DESC";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            List<Payment> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения платежей за период", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}

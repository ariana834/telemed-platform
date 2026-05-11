package com.telemedicina.subscription.repository;

import com.telemedicina.subscription.dto.request.PaymentRequest;
import com.telemedicina.subscription.dto.request.SubscriptionRequest;
import com.telemedicina.subscription.mapper.SubscriptionMapper;
import com.telemedicina.subscription.model.PaymentHistory;
import com.telemedicina.subscription.model.Subscription;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final JdbcTemplate jdbc;
    private final SubscriptionMapper mapper;

    public SubscriptionRepositoryImpl(JdbcTemplate jdbc, SubscriptionMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Long createSubscription(Long patientId, SubscriptionRequest request) {
        // end_date si price sunt omise — triggerul trg_set_subscription_end_date le calculeaza automat
        String sql = """
                INSERT INTO subscriptions (patient_id, type, price)
                VALUES (?, ?::subscription_type, 0)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, patientId);
            ps.setString(2, request.getType());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public Optional<Subscription> findById(Long id) {
        String sql = """
                SELECT id, patient_id, type::text, start_date, end_date,
                       status::text, price, created_at, updated_at
                FROM subscriptions WHERE id = ?
                """;
        List<Subscription> results = jdbc.query(sql, mapper::mapToSubscription, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Subscription> findActiveByPatientId(Long patientId) {
        String sql = """
                SELECT id, patient_id, type::text, start_date, end_date,
                       status::text, price, created_at, updated_at
                FROM subscriptions
                WHERE patient_id = ? AND status = 'ACTIVE' AND end_date >= CURRENT_DATE
                """;
        List<Subscription> results = jdbc.query(sql, mapper::mapToSubscription, patientId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Subscription> findAllByPatientId(Long patientId) {
        String sql = """
                SELECT id, patient_id, type::text, start_date, end_date,
                       status::text, price, created_at, updated_at
                FROM subscriptions
                WHERE patient_id = ?
                ORDER BY created_at DESC
                """;
        return jdbc.query(sql, mapper::mapToSubscription, patientId);
    }

    @Override
    public Long createPayment(Long subscriptionId, PaymentRequest request) {
        // creeaza plata in stare PENDING
        // transaction_id e generat automat daca nu vine din request
        String transactionId = request.getTransactionId() != null
                ? request.getTransactionId()
                : "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // suma e luata direct din abonament
        String sql = """
                INSERT INTO payment_history (subscription_id, amount, payment_method, status, transaction_id)
                SELECT ?, price, ?::varchar, 'PENDING'::payment_status, ?
                FROM subscriptions WHERE id = ?
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, subscriptionId);
            ps.setString(2, request.getPaymentMethod() != null ? request.getPaymentMethod() : "CARD");
            ps.setString(3, transactionId);
            ps.setLong(4, subscriptionId);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void confirmPayment(Long paymentId) {
        // update catre COMPLETED → triggerul trg_activate_subscription_on_payment
        // prinde acest update si activeaza automat abonamentul
        jdbc.update("""
                UPDATE payment_history SET status = 'COMPLETED'::payment_status
                WHERE id = ?
                """, paymentId);
    }

    @Override
    public Optional<PaymentHistory> findPaymentById(Long paymentId) {
        String sql = """
                SELECT id, subscription_id, amount, payment_date,
                       payment_method, status::text, transaction_id, notes
                FROM payment_history WHERE id = ?
                """;
        List<PaymentHistory> results = jdbc.query(sql, mapper::mapToPayment, paymentId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<PaymentHistory> findPaymentsBySubscriptionId(Long subscriptionId) {
        String sql = """
                SELECT id, subscription_id, amount, payment_date,
                       payment_method, status::text, transaction_id, notes
                FROM payment_history
                WHERE subscription_id = ?
                ORDER BY payment_date DESC
                """;
        return jdbc.query(sql, mapper::mapToPayment, subscriptionId);
    }
}
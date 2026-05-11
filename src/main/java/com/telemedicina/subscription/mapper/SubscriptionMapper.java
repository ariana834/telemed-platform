package com.telemedicina.subscription.mapper;

import com.telemedicina.subscription.dto.response.PaymentResponse;
import com.telemedicina.subscription.dto.response.SubscriptionResponse;
import com.telemedicina.subscription.model.PaymentHistory;
import com.telemedicina.subscription.model.Subscription;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class SubscriptionMapper {

    public Subscription mapToSubscription(ResultSet rs, int rowNum) throws SQLException {
        Subscription s = new Subscription();
        s.setId(rs.getLong("id"));
        s.setPatientId(rs.getLong("patient_id"));
        s.setType(rs.getString("type"));
        s.setStatus(rs.getString("status"));
        s.setPrice(rs.getBigDecimal("price"));

        var sd = rs.getDate("start_date");
        if (sd != null) s.setStartDate(sd.toLocalDate());

        var ed = rs.getDate("end_date");
        if (ed != null) s.setEndDate(ed.toLocalDate());

        s.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        s.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return s;
    }

    public SubscriptionResponse toResponse(Subscription s) {
        SubscriptionResponse r = new SubscriptionResponse();
        r.setId(s.getId());
        r.setPatientId(s.getPatientId());
        r.setType(s.getType());
        r.setStartDate(s.getStartDate());
        r.setEndDate(s.getEndDate());
        r.setStatus(s.getStatus());
        r.setPrice(s.getPrice());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }

    public PaymentHistory mapToPayment(ResultSet rs, int rowNum) throws SQLException {
        PaymentHistory p = new PaymentHistory();
        p.setId(rs.getLong("id"));
        p.setSubscriptionId(rs.getLong("subscription_id"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setStatus(rs.getString("status"));
        p.setTransactionId(rs.getString("transaction_id"));
        p.setNotes(rs.getString("notes"));
        p.setPaymentDate(rs.getObject("payment_date", java.time.OffsetDateTime.class));
        return p;
    }

    public PaymentResponse toPaymentResponse(PaymentHistory p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setSubscriptionId(p.getSubscriptionId());
        r.setAmount(p.getAmount());
        r.setPaymentDate(p.getPaymentDate());
        r.setPaymentMethod(p.getPaymentMethod());
        r.setStatus(p.getStatus());
        r.setTransactionId(p.getTransactionId());
        return r;
    }
}
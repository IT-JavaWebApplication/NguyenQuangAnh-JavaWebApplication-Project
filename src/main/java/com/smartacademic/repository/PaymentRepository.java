package com.smartacademic.repository;

import com.smartacademic.entity.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    void update(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByTxnRef(String txnRef);
    Optional<Payment> findLatestPendingBySessionId(Long sessionId);
    List<Payment> findByPayerId(Long payerId);
}

package com.vupl.paymentservice.repository;

import com.vupl.paymentservice.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPaymentIdOrderByCreatedAtAsc(String paymentId);
}

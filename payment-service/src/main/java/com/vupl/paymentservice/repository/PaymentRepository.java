package com.vupl.paymentservice.repository;

import com.vupl.paymentservice.entity.Payment;
import com.vupl.paymentservice.entity.Payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByGatewayRef(String gatewayRef);

    List<Payment> findByUserId(String userId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByOrderId(String orderId);
}

package com.vupl.orderservice.repository;

import com.vupl.orderservice.entity.Order;
import com.vupl.orderservice.entity.Order.OrderStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    Optional<Order> findByOrderCode(String orderCode);

    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND (:status IS NULL OR o.status = :status) ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdAndStatus(@Param("userId") String userId,
                                      @Param("status") OrderStatus status,
                                      Pageable pageable);
}

package org.example.petinside.domain.payment.repository;

import org.example.petinside.domain.payment.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}

package org.example.arp_ecurity_layer.guard.repository;

import org.example.arp_ecurity_layer.guard.entity.SavingGuardMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SavingGuardMetricRepository extends JpaRepository<SavingGuardMetric, Long> {

    List<SavingGuardMetric> findByCustomerIdAndCreatedAtBetween(Long customerId, LocalDateTime start, LocalDateTime end);

    long countByCustomerIdAndCreatedAtBetween(Long customerId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(m.savedAmount), 0) FROM SavingGuardMetric m WHERE m.customerId = :customerId AND m.createdAt BETWEEN :start AND :end")
    BigDecimal sumSavedAmountByCustomerIdAndCreatedAtBetween(
            @Param("customerId") Long customerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}

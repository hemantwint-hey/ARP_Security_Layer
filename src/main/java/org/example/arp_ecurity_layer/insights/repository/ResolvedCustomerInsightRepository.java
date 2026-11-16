package org.example.arp_ecurity_layer.insights.repository;

import org.example.arp_ecurity_layer.insights.entity.ResolvedCustomerInsight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResolvedCustomerInsightRepository extends JpaRepository<ResolvedCustomerInsight, Long> {

    Page<ResolvedCustomerInsight> findByCustomerId(Long customerId, Pageable pageable);

    boolean existsBySourceEventId(String sourceEventId);
}


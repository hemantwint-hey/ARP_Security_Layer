package org.example.arp_ecurity_layer.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GatewayProcessedEventRepository extends JpaRepository<GatewayProcessedEvent, String> {

    boolean existsByEventId(String eventId);
}


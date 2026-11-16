package org.example.arp_ecurity_layer.privacy.repository;

import org.example.arp_ecurity_layer.privacy.entity.PrivacyAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivacyAuditLogRepository extends JpaRepository<PrivacyAuditLog, Long> {
}


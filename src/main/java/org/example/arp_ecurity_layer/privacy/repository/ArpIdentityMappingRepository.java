package org.example.arp_ecurity_layer.privacy.repository;

import org.example.arp_ecurity_layer.privacy.entity.ArpIdentityMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArpIdentityMappingRepository extends JpaRepository<ArpIdentityMapping, Long> {

    Optional<ArpIdentityMapping> findByCustomerIdAndStatus(Long customerId, String status);

    Optional<ArpIdentityMapping> findByArpTokenAndStatus(String arpToken, String status);

    boolean existsByArpToken(String arpToken);
}


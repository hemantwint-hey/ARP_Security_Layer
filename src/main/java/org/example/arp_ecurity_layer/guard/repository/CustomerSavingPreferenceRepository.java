package org.example.arp_ecurity_layer.guard.repository;

import org.example.arp_ecurity_layer.guard.entity.CustomerSavingPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerSavingPreferenceRepository extends JpaRepository<CustomerSavingPreference, Long> {
}


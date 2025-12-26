package com.polls.backend.repository;

import com.polls.backend.entity.DeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface DeviceFingerprintRepository extends JpaRepository<DeviceFingerprint, UUID> {
    Optional<DeviceFingerprint> findByFingerprintHash(String fingerprintHash);
    List<DeviceFingerprint> findByIsBlockedTrue();
    boolean existsByFingerprintHash(String fingerprintHash);
}
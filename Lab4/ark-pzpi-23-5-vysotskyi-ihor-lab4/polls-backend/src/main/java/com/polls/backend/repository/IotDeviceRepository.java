
package com.polls.backend.repository;

import com.polls.backend.entity.IotDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IotDeviceRepository extends JpaRepository<IotDevice, UUID> {
    Optional<IotDevice> findByKioskId(String kioskId);
    List<IotDevice> findByIsActive(Boolean isActive);
}
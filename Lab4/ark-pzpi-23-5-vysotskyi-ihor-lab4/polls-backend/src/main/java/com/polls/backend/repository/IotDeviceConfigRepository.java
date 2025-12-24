package com.polls.backend.repository;

import com.polls.backend.entity.IotDeviceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface IotDeviceConfigRepository extends JpaRepository<IotDeviceConfig, UUID> {
    Optional<IotDeviceConfig> findByIotDeviceId(UUID iotDeviceId);
}
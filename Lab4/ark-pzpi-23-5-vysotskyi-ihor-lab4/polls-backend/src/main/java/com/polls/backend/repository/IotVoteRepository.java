package com.polls.backend.repository;

import com.polls.backend.entity.IotVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IotVoteRepository extends JpaRepository<IotVote, UUID> {
    List<IotVote> findByIotDeviceId(UUID iotDeviceId);
    List<IotVote> findByPollId(UUID pollId);
    List<IotVote> findByValidationStatus(String validationStatus);
    long countByIotDeviceId(UUID iotDeviceId);
    long countByPollId(UUID pollId);
}
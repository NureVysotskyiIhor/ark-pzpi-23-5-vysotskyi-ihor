package com.polls.backend.repository;

import com.polls.backend.entity.Poll;
import com.polls.backend.entity.DeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PollRepository extends JpaRepository<Poll, UUID> {
    List<Poll> findByOrganizerFingerprint(DeviceFingerprint organizer);
    List<Poll> findByStatus(String status);
    List<Poll> findByType(String type);
    List<Poll> findByOrganizerFingerprintAndStatus(DeviceFingerprint organizer, String status);
}

package com.polls.backend.repository;

import com.polls.backend.entity.PollOption;
import com.polls.backend.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
    List<PollOption> findByPollOrderByOrderNum(Poll poll);
}
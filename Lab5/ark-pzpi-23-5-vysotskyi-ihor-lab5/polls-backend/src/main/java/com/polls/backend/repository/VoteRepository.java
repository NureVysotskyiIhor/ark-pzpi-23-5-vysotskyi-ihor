package com.polls.backend.repository;

import com.polls.backend.entity.Vote;
import com.polls.backend.entity.Poll;
import com.polls.backend.entity.DeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    List<Vote> findByPoll(Poll poll);
    List<Vote> findByFingerprint(DeviceFingerprint fingerprint);
    Optional<Vote> findByPollAndFingerprint(Poll poll, DeviceFingerprint fingerprint);
    long countByPoll(Poll poll);

    // ИСПРАВЛЕНО — теперь работает!
    long countByPollAndOption_Id(Poll poll, UUID optionId);

    // Или ещё надёжнее — явный запрос
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.poll = :poll AND v.option.id = :optionId")
    long countVotesForOption(@Param("poll") Poll poll, @Param("optionId") UUID optionId);
}
package com.polls.backend.controller;

import com.polls.backend.entity.Vote;
import com.polls.backend.repository.VoteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/votes")
@Tag(name = "Votes", description = "Управління голосами")
public class VoteController {

    @Autowired
    private VoteRepository voteRepository;

    @GetMapping
    @Operation(summary = "Отримати всі голоси")
    public ResponseEntity<List<Vote>> getAllVotes() {
        return ResponseEntity.ok(voteRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Отримати голос за ID")
    public ResponseEntity<Vote> getVoteById(@PathVariable UUID id) {
        return voteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Створити новий голос")
    public ResponseEntity<Vote> createVote(@RequestBody Vote vote) {
        Vote savedVote = voteRepository.save(vote);
        return ResponseEntity.ok(savedVote);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити голос")
    public ResponseEntity<Void> deleteVote(@PathVariable UUID id) {
        if (voteRepository.existsById(id)) {
            voteRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

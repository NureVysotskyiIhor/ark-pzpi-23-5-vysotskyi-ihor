package com.polls.backend.controller;

import com.polls.backend.entity.Poll;
import com.polls.backend.repository.PollRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/polls")
@Tag(name = "Polls", description = "Управління голосуваннями")
public class PollController {

    @Autowired
    private PollRepository pollRepository;

    @GetMapping
    @Operation(summary = "Отримати всі голосування")
    public ResponseEntity<List<Poll>> getAllPolls() {
        return ResponseEntity.ok(pollRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Отримати голосування за ID")
    public ResponseEntity<Poll> getPollById(@PathVariable UUID id) {
        return pollRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Створити нове голосування")
    public ResponseEntity<Poll> createPoll(@RequestBody Poll poll) {
        Poll savedPoll = pollRepository.save(poll);
        return ResponseEntity.ok(savedPoll);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Оновити голосування")
    public ResponseEntity<Poll> updatePoll(@PathVariable UUID id, @RequestBody Poll pollDetails) {
        return pollRepository.findById(id)
                .map(poll -> {
                    poll.setTitle(pollDetails.getTitle());
                    poll.setQuestion(pollDetails.getQuestion());
                    poll.setStatus(pollDetails.getStatus());
                    return ResponseEntity.ok(pollRepository.save(poll));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити голосування")
    public ResponseEntity<Void> deletePoll(@PathVariable UUID id) {
        if (pollRepository.existsById(id)) {
            pollRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Отримати голосування за статусом")
    public ResponseEntity<List<Poll>> getPollsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(pollRepository.findByStatus(status));
    }
}

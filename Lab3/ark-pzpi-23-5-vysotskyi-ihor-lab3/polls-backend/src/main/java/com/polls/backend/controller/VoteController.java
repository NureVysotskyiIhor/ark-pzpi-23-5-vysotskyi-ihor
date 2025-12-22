package com.polls.backend.controller;

import com.polls.backend.dto.ApiErrorResponseDTO;
import com.polls.backend.dto.CreateVoteRequestDTO;
import com.polls.backend.entity.Vote;
import com.polls.backend.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/votes")
@Tag(name = "Votes", description = "Управління голосами")
public class VoteController {

    @Autowired
    private VoteService voteService;

    /**
     * Отримати всі голоси
     */
    @GetMapping
    @Operation(summary = "Отримати всі голоси")
    public ResponseEntity<List<Vote>> getAllVotes() {
        List<Vote> votes = voteService.getAll();
        return ResponseEntity.ok(votes);
    }

    /**
     * Отримати голос за ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Отримати голос за ID")
    public ResponseEntity<Vote> getVoteById(@PathVariable UUID id) {
        Vote vote = voteService.getVoteById(id);
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vote);
    }

    /**
     * Зареєструвати новий голос з валідацією
     * РЕФАКТОРИНГ: @Valid активує всі аннотації в DTO
     * БІЗНЕС-ЛОГІКА:
     * - Перевірка на повторне голосування
     * - Перевірка на блокування пристрою
     * - Отримання або створення device fingerprint
     */
    @PostMapping
    @Operation(summary = "Зареєструвати голос")
    public ResponseEntity<?> createVote(
            @Valid @RequestBody CreateVoteRequestDTO request) {

        try {
            Vote vote = voteService.registerVote(
                    UUID.fromString(request.getPollId()),
                    request.getOptionId() != null ? UUID.fromString(request.getOptionId()) : null,
                    UUID.fromString(request.getFingerprintId())
            );

            if (vote == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiErrorResponseDTO(
                                "CONFLICT",
                                "Ви вже голосували або ваш пристрій заблокований",
                                java.time.LocalDateTime.now(),
                                "/api/votes",
                                null,
                                "DUPLICATE_VOTE"
                        ));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(vote);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Видалити голос адміністратором
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити голос")
    public ResponseEntity<Void> deleteVote(@PathVariable UUID id) {
        boolean deleted = voteService.deleteVote(id, UUID.randomUUID()); // adminId передати з контексту
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
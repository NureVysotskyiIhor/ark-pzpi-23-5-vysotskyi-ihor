// File: src/main/java/com/polls/backend/controller/PollOptionController.java

package com.polls.backend.controller;

import com.polls.backend.entity.PollOption;
import com.polls.backend.entity.Poll;
import com.polls.backend.repository.PollOptionRepository;
import com.polls.backend.repository.PollRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/poll-options")
@Tag(name = "Poll Options", description = "Управління варіантами відповідей")
public class PollOptionController {

    @Autowired
    private PollOptionRepository pollOptionRepository;

    @Autowired
    private PollRepository pollRepository;

    @GetMapping
    @Operation(summary = "Отримати всі варіанти")
    public ResponseEntity<List<PollOption>> getAllOptions() {
        return ResponseEntity.ok(pollOptionRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Отримати варіант за ID")
    public ResponseEntity<PollOption> getOptionById(@PathVariable UUID id) {
        return pollOptionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Створити новий варіант")
    public ResponseEntity<?> createOption(@RequestBody CreatePollOptionRequest request) {
        // Знайди poll за ID
        Optional<Poll> poll = pollRepository.findById(request.getPollId());
        if (poll.isEmpty()) {
            return ResponseEntity.badRequest().body("Poll not found");
        }

        // Створи варіант
        PollOption option = new PollOption();
        option.setPoll(poll.get());
        option.setText(request.getText());
        option.setOrderNum(request.getOrderNum());

        PollOption savedOption = pollOptionRepository.save(option);
        return ResponseEntity.ok(savedOption);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити варіант")
    public ResponseEntity<Void> deleteOption(@PathVariable UUID id) {
        if (pollOptionRepository.existsById(id)) {
            pollOptionRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // DTO для создания
    public static class CreatePollOptionRequest {
        private UUID pollId;
        private String text;
        private Integer orderNum;

        public UUID getPollId() { return pollId; }
        public void setPollId(UUID pollId) { this.pollId = pollId; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Integer getOrderNum() { return orderNum; }
        public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    }
}
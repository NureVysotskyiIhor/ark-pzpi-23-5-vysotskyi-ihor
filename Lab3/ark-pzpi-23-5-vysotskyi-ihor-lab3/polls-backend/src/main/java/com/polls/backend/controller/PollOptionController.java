package com.polls.backend.controller;

import com.polls.backend.dto.CreatePollOptionRequestDTO;
import com.polls.backend.entity.PollOption;
import com.polls.backend.service.PollOptionService;
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
@RequestMapping("/api/poll-options")
@Tag(name = "Poll Options", description = "Управління варіантами відповідей")
public class PollOptionController {

    @Autowired
    private PollOptionService pollOptionService;

    /**
     * Отримати всі варіанти
     */
    @GetMapping
    @Operation(summary = "Отримати всі варіанти")
    public ResponseEntity<List<PollOption>> getAllOptions() {
        List<PollOption> options = pollOptionService.getAll();
        return ResponseEntity.ok(options);
    }

    /**
     * Отримати варіант за ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Отримати варіант за ID")
    public ResponseEntity<PollOption> getOptionById(@PathVariable UUID id) {
        PollOption option = pollOptionService.getOptionById(id);
        if (option == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(option);
    }

    /**
     * Додати новий варіант з автоматичним orderNum
     * РЕФАКТОРИНГ: @Valid валідує CreatePollOptionRequestDTO
     * МАТЕМАТИКА: orderNum = max(existing) + 1
     */
    @PostMapping
    @Operation(summary = "Додати варіант")
    public ResponseEntity<PollOption> createOption(
            @Valid @RequestBody CreatePollOptionRequestDTO request) {

        try {
            // Використовуємо автоматичне призначення orderNum
            PollOption option = pollOptionService.addOption(
                    UUID.fromString(request.getPollId()),
                    request.getText()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(option);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Додати варіант з явним orderNum (з валідацією дублів)
     * РЕФАКТОРИНГ: Валідація запобігає дублювання orderNum
     */
    @PostMapping("/with-order")
    @Operation(summary = "Додати варіант з явним порядком")
    public ResponseEntity<PollOption> createOptionWithOrder(
            @Valid @RequestBody CreatePollOptionRequestDTO request) {

        try {
            PollOption option = pollOptionService.addOptionWithOrder(
                    UUID.fromString(request.getPollId()),
                    request.getText(),
                    request.getOrderNum()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(option);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Видалити варіант
     * РЕФАКТОРИНГ: Автоматичне переупорядкування інших варіантів
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити варіант")
    public ResponseEntity<Void> deleteOption(@PathVariable UUID id) {
        boolean deleted = pollOptionService.deleteOption(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        // Автоматично переупорядковуємо
        // pollOptionService.reorderOptions(pollId); // якщо потрібно
        return ResponseEntity.noContent().build();
    }
}
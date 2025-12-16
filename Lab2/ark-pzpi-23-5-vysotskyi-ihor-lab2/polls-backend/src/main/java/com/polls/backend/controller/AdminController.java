package com.polls.backend.controller;

import com.polls.backend.entity.Admin;
import com.polls.backend.repository.AdminRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admins")
@Tag(name = "Admins", description = "Управління адміністраторами")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @GetMapping
    @Operation(summary = "Отримати всіх адміністраторів")
    public ResponseEntity<List<Admin>> getAllAdmins() {
        return ResponseEntity.ok(adminRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Отримати адміністратора за ID")
    public ResponseEntity<Admin> getAdminById(@PathVariable UUID id) {
        return adminRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Створити нового адміністратора")
    public ResponseEntity<Admin> createAdmin(@RequestBody Admin admin) {
        Admin savedAdmin = adminRepository.save(admin);
        return ResponseEntity.ok(savedAdmin);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Оновити адміністратора")
    public ResponseEntity<Admin> updateAdmin(@PathVariable UUID id, @RequestBody Admin adminDetails) {
        return adminRepository.findById(id)
                .map(admin -> {
                    admin.setName(adminDetails.getName());
                    admin.setIsActive(adminDetails.getIsActive());
                    return ResponseEntity.ok(adminRepository.save(admin));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити адміністратора")
    public ResponseEntity<Void> deleteAdmin(@PathVariable UUID id) {
        if (adminRepository.existsById(id)) {
            adminRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

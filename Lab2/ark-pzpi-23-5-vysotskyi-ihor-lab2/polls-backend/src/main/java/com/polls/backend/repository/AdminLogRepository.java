package com.polls.backend.repository;

import com.polls.backend.entity.AdminLog;
import com.polls.backend.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, UUID> {
    List<AdminLog> findByAdmin(Admin admin);
    List<AdminLog> findByAction(String action);
    List<AdminLog> findByTargetType(String targetType);
}

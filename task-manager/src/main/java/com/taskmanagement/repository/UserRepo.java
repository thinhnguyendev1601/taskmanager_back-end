package com.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.taskmanagement.entity.User;

public interface UserRepo extends JpaRepository<User, Long> {
}
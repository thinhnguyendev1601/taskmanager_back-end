package com.taskmanagement.controller;

import com.taskmanagement.entity.Task;
import com.taskmanagement.dto.TaskDTO;
import com.taskmanagement.dto.TaskSimpleDTO;
import com.taskmanagement.dto.CreateTaskRequest;
import com.taskmanagement.dto.UpdateTaskRequest;
import com.taskmanagement.repository.TaskRepo;
import com.taskmanagement.repository.UserRepo;
import com.taskmanagement.repository.CategoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    private final TaskRepo taskRepository;
    private final UserRepo userRepository;
    private final CategoryRepo categoryRepository;

    // Get all active tasks (for Kanban board)
    @GetMapping("/tasks")
    public List<TaskSimpleDTO> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();

        return tasks.stream()
                .filter(task -> !task.isDeleted())
                .map(this::convertToSimpleDTO)
                .collect(Collectors.toList());
    }

    // Get single task with full details
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Integer id) {
        return taskRepository.findById(id)
                .map(this::convertToFullDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new task
    @PostMapping("/tasks")
    public ResponseEntity<TaskDTO> createTask(@RequestBody CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(Task.TaskStatus.valueOf(request.getStatus()));
        task.setPriority(Task.TaskPriority.valueOf(request.getPriority()));
        task.setStartDate(request.getStartDate());
        task.setDueDate(request.getDueDate());
        task.setDeleted(false);

        // Set category if provided
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .ifPresent(task::setCategory);
        }

        // Set created by (in real app, get from security context)
        userRepository.findById(request.getCreatedById())
                .ifPresent(task::setCreatedBy);

        Task savedTask = taskRepository.save(task);
        return ResponseEntity.ok(convertToFullDTO(savedTask));
    }

    // Update task
    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Integer id,
                                              @RequestBody UpdateTaskRequest request) {
        return taskRepository.findById(id)
                .map(task -> {
                    if (request.getTitle() != null) task.setTitle(request.getTitle());
                    if (request.getDescription() != null) task.setDescription(request.getDescription());
                    if (request.getStatus() != null) task.setStatus(Task.TaskStatus.valueOf(request.getStatus()));
                    if (request.getPriority() != null) task.setPriority(Task.TaskPriority.valueOf(request.getPriority()));
                    if (request.getStartDate() != null) task.setStartDate(request.getStartDate());
                    if (request.getDueDate() != null) task.setDueDate(request.getDueDate());

                    Task updatedTask = taskRepository.save(task);
                    return ResponseEntity.ok(convertToFullDTO(updatedTask));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Soft delete task (move to trash)
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Integer id) {
        return taskRepository.findById(id)
                .map(task -> {
                    task.setDeleted(true);
                    task.setDeletedAt(LocalDateTime.now());
                    taskRepository.save(task);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Get tasks in trash
    @GetMapping("/trash")
    public List<TaskSimpleDTO> getDeletedTasks() {
        return taskRepository.findAll().stream()
                .filter(Task::isDeleted)
                .map(this::convertToSimpleDTO)
                .collect(Collectors.toList());
    }

    // Restore task from trash
    @PutMapping("/tasks/{id}/restore")
    public ResponseEntity<TaskDTO> restoreTask(@PathVariable Integer id) {
        return taskRepository.findById(id)
                .map(task -> {
                    task.setDeleted(false);
                    task.setDeletedAt(null);
                    Task restoredTask = taskRepository.save(task);
                    return ResponseEntity.ok(convertToFullDTO(restoredTask));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Get tasks by status (for dashboard statistics)
    @GetMapping("/tasks/by-status/{status}")
    public List<TaskSimpleDTO> getTasksByStatus(@PathVariable String status) {
        Task.TaskStatus taskStatus = Task.TaskStatus.valueOf(status.toUpperCase());
        return taskRepository.findAll().stream()
                .filter(task -> !task.isDeleted() && task.getStatus() == taskStatus)
                .map(this::convertToSimpleDTO)
                .collect(Collectors.toList());
    }

    // Convert to simple DTO (for lists)
    private TaskSimpleDTO convertToSimpleDTO(Task task) {
        return TaskSimpleDTO.builder()
                .taskId(task.getTaskId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().toString())
                .priority(task.getPriority().toString())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .categoryName(task.getCategory() != null ? task.getCategory().getName() : null)
                .createdByUsername(task.getCreatedBy() != null ? task.getCreatedBy().getUsername() : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // Convert to full DTO (for single task details)
    private TaskDTO convertToFullDTO(Task task) {
        return TaskDTO.builder()
                .taskId(task.getTaskId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().toString())
                .priority(task.getPriority().toString())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .categoryName(task.getCategory() != null ? task.getCategory().getName() : null)
                .createdByUsername(task.getCreatedBy() != null ? task.getCreatedBy().getUsername() : null)
                // For now, leave these empty - we'll add them later when needed
                .assignedUsers(List.of())
                .tags(List.of())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}


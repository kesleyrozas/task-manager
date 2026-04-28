package com.taskmanager.domain.task;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    long countByAssigneeIdAndStatus(Long assigneeId, Status status);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.assignee.id = :assigneeId AND t.status = :status
            """)
    long countInProgressForUpdate(@Param("assigneeId") Long assigneeId, @Param("status") Status status);

    @Query("""
            SELECT t FROM Task t
            LEFT JOIN FETCH t.assignee
            LEFT JOIN FETCH t.project
            WHERE t.id = :id
            """)
    Optional<Task> findByIdWithRelations(Long id);

    @Query("""
            SELECT t.status AS status, COUNT(t) AS total
            FROM Task t
            WHERE t.project.id = :projectId
            GROUP BY t.status
            """)
    List<StatusCount> countByStatusForProject(Long projectId);

    @Query("""
            SELECT t.priority AS priority, COUNT(t) AS total
            FROM Task t
            WHERE t.project.id = :projectId
            GROUP BY t.priority
            """)
    List<PriorityCount> countByPriorityForProject(Long projectId);

    interface StatusCount {
        Status getStatus();
        Long getTotal();
    }

    interface PriorityCount {
        Priority getPriority();
        Long getTotal();
    }
}

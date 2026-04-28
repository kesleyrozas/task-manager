package com.taskmanager.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
            SELECT DISTINCT p FROM Project p
            LEFT JOIN p.members m
            WHERE p.owner.id = :userId OR m.id = :userId
            """)
    List<Project> findAllAccessibleBy(Long userId);

    @Query("""
            SELECT p FROM Project p
            LEFT JOIN FETCH p.members
            LEFT JOIN FETCH p.owner
            WHERE p.id = :id
            """)
    Optional<Project> findByIdWithMembers(Long id);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM Project p LEFT JOIN p.members m
            WHERE p.id = :projectId AND (p.owner.id = :userId OR m.id = :userId)
            """)
    boolean isAccessibleBy(Long projectId, Long userId);
}

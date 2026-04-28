package com.taskmanager.domain.task;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TaskSpecifications {

    private TaskSpecifications() {}

    public static Specification<Task> withFilters(
            Long projectId,
            Status status,
            Priority priority,
            Long assigneeId,
            Instant deadlineFrom,
            Instant deadlineTo,
            String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (deadlineFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deadline"), deadlineFrom));
            }
            if (deadlineTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deadline"), deadlineTo));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), like);
                Predicate descMatch  = cb.like(cb.lower(root.get("description")), like);
                predicates.add(cb.or(titleMatch, descMatch));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

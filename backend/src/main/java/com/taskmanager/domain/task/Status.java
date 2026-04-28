package com.taskmanager.domain.task;

import java.util.Set;

public enum Status {
    TODO,
    IN_PROGRESS,
    DONE;

    public boolean canTransitionTo(Status next) {
        if (this == next) {
            return true;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }

    private static final java.util.Map<Status, Set<Status>> ALLOWED_TRANSITIONS = java.util.Map.of(
            TODO,        Set.of(IN_PROGRESS, DONE),
            IN_PROGRESS, Set.of(TODO, DONE),
            DONE,        Set.of(IN_PROGRESS)
    );
}

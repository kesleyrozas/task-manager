package com.taskmanager.api.dto.project;

import com.taskmanager.domain.user.User;

public record MemberSummary(Long id, String name, String email) {

    public static MemberSummary from(User user) {
        if (user == null) {
            return null;
        }
        return new MemberSummary(user.getId(), user.getName(), user.getEmail());
    }
}

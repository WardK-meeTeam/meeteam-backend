package com.wardk.meeteam_backend.domain.notification.entity;

public enum NotificationType {

    PROJECT_APPLY(true),     // actor 필요
    PROJECT_MY_APPLY(false), // actor 불필요
    PROJECT_APPROVE(false),
    PROJECT_REJECT(false);

    private final boolean requiresActor;

    NotificationType(boolean requiresActor) {
        this.requiresActor = requiresActor;
    }

    public boolean requiresActor() {
        return requiresActor;
    }


}

package com.wardk.meeteam_backend.domain.notification.entity;

public enum NotificationType {

    PROJECT_APPLY(true),     // actor 필요
    PROJECT_SELF_APPLY(false), // actor 불필요
    PROJECT_APPLICATION_APPROVED(false),
    PROJECT_APPLICATION_REJECTED(false),
    PROJECT_ENDED(false);


    private final boolean requiresActor;

    NotificationType(boolean requiresActor) {
        this.requiresActor = requiresActor;
    }

    public boolean requiresActor() {
        return requiresActor;
    }


}

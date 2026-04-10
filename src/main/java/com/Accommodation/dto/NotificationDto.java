package com.Accommodation.dto;

import com.Accommodation.entity.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationDto {

    private final Long id;
    private final String message;
    private final String targetUrl;
    private final boolean read;
    private final LocalDateTime createdAt;

    public NotificationDto(Notification notification) {
        this(notification, notification.getTargetUrl());
    }

    public NotificationDto(Notification notification, String targetUrl) {
        this.id = notification.getId();
        this.message = notification.getMessage();
        this.targetUrl = targetUrl;
        this.read = notification.isRead();
        this.createdAt = notification.getRegTime();
    }
}

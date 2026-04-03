package com.Accommodation.controller;

import com.Accommodation.dto.NotificationDto;
import com.Accommodation.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications/stream")
    @ResponseBody
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        return notificationService.subscribe(userDetails.getUsername());
    }

    @GetMapping("/notifications/recent")
    @ResponseBody
    public List<NotificationDto> recentNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        return notificationService.getRecentNotifications(userDetails.getUsername());
    }

    @PostMapping("/notifications/{notificationId}/read")
    @ResponseBody
    public void markAsRead(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable("notificationId") Long notificationId) {
        notificationService.markAsRead(userDetails.getUsername(), notificationId);
    }

    @GetMapping("/notifications/{notificationId}/open")
    public RedirectView openNotification(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable("notificationId") Long notificationId) {
        String targetUrl = notificationService.markAsReadAndGetTargetUrl(userDetails.getUsername(), notificationId);
        return new RedirectView(targetUrl);
    }
}

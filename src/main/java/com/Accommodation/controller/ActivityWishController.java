package com.Accommodation.controller;

import com.Accommodation.dto.ActivityWishDto;
import com.Accommodation.service.ActivityWishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ActivityWishController {

    private final ActivityWishService activityWishService;

    @PostMapping("/activity-wish")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addWish(@RequestBody ActivityWishDto request,
                                                       Principal principal) {
        ensureAuthenticated(principal);
        activityWishService.addWish(request, principal.getName());
        return ResponseEntity.ok(Map.of("wished", true));
    }

    @DeleteMapping("/activity-wish/{activityKey}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeWish(@PathVariable String activityKey,
                                                          Principal principal) {
        ensureAuthenticated(principal);
        activityWishService.removeWish(activityKey, principal.getName());
        return ResponseEntity.ok(Map.of("wished", false));
    }

    private void ensureAuthenticated(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}

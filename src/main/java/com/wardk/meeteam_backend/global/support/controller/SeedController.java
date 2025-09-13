package com.wardk.meeteam_backend.global.support.controller;


import com.wardk.meeteam_backend.global.support.service.SeedSevice;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Profile("local")
@RestController
@RequestMapping("/dev/seed")
@RequiredArgsConstructor
@Validated
public class SeedController {


    private final SeedSevice seedService;

    /** 예: POST /dev/seed/projects?count=50 */
    @PostMapping("/projects")
    public Map<String, Object> seedProjects(
            @RequestParam(defaultValue = "30") @Min(1) @Max(1000000) int count
    ) {
        List<Long> ids = seedService.seedProjects(count);
        return Map.of(
                "created", ids.size(),
                "projectIds", ids
        );
    }
}

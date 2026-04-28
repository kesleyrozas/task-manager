package com.taskmanager.api.controller;

import com.taskmanager.api.dto.report.ProjectReportResponse;
import com.taskmanager.application.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/projects/{projectId}/report")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Summary counts by status and priority for a project")
    @GetMapping
    public ProjectReportResponse summary(@PathVariable Long projectId) {
        return reportService.summary(projectId);
    }
}

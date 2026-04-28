package com.taskmanager.api.dto.report;

import java.util.Map;

public record ProjectReportResponse(
        Long projectId,
        Map<String, Long> byStatus,
        Map<String, Long> byPriority
) {}

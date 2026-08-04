package com.pms.dashboard.service;

import com.pms.dashboard.dto.response.FrontdeskDashboardResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface FrontdeskDashboardService {

    FrontdeskDashboardResponse getDashboard(UUID propertyId, LocalDate businessDate);
}


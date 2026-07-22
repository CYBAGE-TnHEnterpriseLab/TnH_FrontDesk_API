package com.pms.property.domain.content.service;

import com.pms.property.domain.content.dto.ContentOverviewRequest;
import com.pms.property.domain.content.dto.ContentOverviewResponse;
import com.pms.property.domain.content.dto.ContentSummaryResponse;
import java.util.List;

public interface ContentService {

    ContentSummaryResponse getSummaryByPropertyId(String propertyId);

    List<ContentOverviewResponse> listOverviewsByPropertyId(String propertyId);

    ContentOverviewResponse getOverviewById(String propertyId, Long overviewId);

    ContentOverviewResponse createOverview(String propertyId, ContentOverviewRequest request);

    ContentOverviewResponse updateOverview(String propertyId, Long overviewId, ContentOverviewRequest request);

    void deleteOverview(String propertyId, Long overviewId);
}



package com.pms.property.publish.facade;

import com.pms.property.publish.dto.PublishResponse;
import com.pms.property.publish.service.PublishService;
import com.pms.common.utils.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class PublishFacade {

    private final PublishService publishService;
    private final HttpServletRequest request;

    public PublishFacade(PublishService publishService, HttpServletRequest request) {
        this.publishService = publishService;
        this.request = request;
    }

    public PublishResponse publish(Long draftId) {
        String authHeader = request.getHeader("Authorization");
        UUID actor = CurrentUser.userId();
        return publishService.publish(draftId, actor, authHeader);
    }
}


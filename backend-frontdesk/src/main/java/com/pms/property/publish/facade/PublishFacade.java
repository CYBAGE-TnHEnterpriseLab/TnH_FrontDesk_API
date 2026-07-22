package com.pms.property.publish.facade;

import com.pms.property.publish.dto.PublishResponse;
import com.pms.property.publish.service.PublishService;
import org.springframework.stereotype.Component;

@Component
public class PublishFacade {

    private final PublishService publishService;

    public PublishFacade(PublishService publishService) {
        this.publishService = publishService;
    }

    public PublishResponse publish(Long draftId, String actor) {
        return publishService.publish(draftId, actor);
    }
}


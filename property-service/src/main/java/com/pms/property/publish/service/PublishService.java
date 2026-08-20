package com.pms.property.publish.service;

import com.pms.property.publish.dto.PublishResponse;

public interface PublishService {

    PublishResponse publish(Long draftId, String actor);
}

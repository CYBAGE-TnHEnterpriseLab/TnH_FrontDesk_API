package com.pms.property.publish.service;

import com.pms.property.publish.dto.PublishResponse;
import java.util.UUID;

public interface PublishService {

    PublishResponse publish(Long draftId, UUID actor, String authHeader);
}

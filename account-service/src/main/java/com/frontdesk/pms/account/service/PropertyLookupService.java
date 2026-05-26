package com.frontdesk.pms.account.service;

import java.util.UUID;

public interface PropertyLookupService {
    boolean exists(UUID propertyId);
}

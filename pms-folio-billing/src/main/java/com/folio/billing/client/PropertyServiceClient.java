package com.folio.billing.client;

import java.util.List;

public interface PropertyServiceClient {

    List<String> getChargeTypes(String propertyId);
}

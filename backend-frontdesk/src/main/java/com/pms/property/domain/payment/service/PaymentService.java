package com.pms.property.domain.payment.service;

import com.pms.property.domain.payment.dto.PaymentMethodRequest;
import com.pms.property.domain.payment.dto.PaymentMethodResponse;
import com.pms.property.domain.payment.dto.PaymentSummaryResponse;
import java.util.List;

public interface PaymentService {

    PaymentSummaryResponse getSummaryByPropertyId(String propertyId);

    List<PaymentMethodResponse> listMethodsByPropertyId(String propertyId);

    PaymentMethodResponse getMethodById(String propertyId, Long methodId);

    PaymentMethodResponse createMethod(String propertyId, PaymentMethodRequest request);

    PaymentMethodResponse updateMethod(String propertyId, Long methodId, PaymentMethodRequest request);

    void deleteMethod(String propertyId, Long methodId);
}



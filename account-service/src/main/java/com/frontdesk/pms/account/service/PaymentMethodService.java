package com.frontdesk.pms.account.service;

import com.frontdesk.pms.account.dto.PaymentMethodRequestDTO;
import com.frontdesk.pms.account.dto.PaymentMethodResponseDTO;
import java.util.List;
import java.util.UUID;

public interface PaymentMethodService {
    PaymentMethodResponseDTO create(PaymentMethodRequestDTO request);
    List<PaymentMethodResponseDTO> list(UUID propertyId);
    PaymentMethodResponseDTO update(UUID id, PaymentMethodRequestDTO request);
    void delete(UUID id);
}

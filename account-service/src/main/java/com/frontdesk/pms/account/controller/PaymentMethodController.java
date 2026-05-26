package com.frontdesk.pms.account.controller;

import com.frontdesk.pms.account.dto.PaymentMethodRequestDTO;
import com.frontdesk.pms.account.dto.PaymentMethodResponseDTO;
import com.frontdesk.pms.account.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/properties/{propertyId}/payment-methods")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;


    @PostMapping
    public PaymentMethodResponseDTO create(@PathVariable UUID propertyId, @RequestBody @Valid PaymentMethodRequestDTO request) {
        request.setPropertyId(propertyId);
        return paymentMethodService.create(request);
    }

    @GetMapping
    public List<PaymentMethodResponseDTO> list(@PathVariable UUID propertyId) {
        return paymentMethodService.list(propertyId);
    }

    @PutMapping("/{id}")
    public PaymentMethodResponseDTO update(@PathVariable UUID propertyId, @PathVariable UUID id, @RequestBody @Valid PaymentMethodRequestDTO request) {
        request.setPropertyId(propertyId);
        return paymentMethodService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        paymentMethodService.delete(id);
    }
}

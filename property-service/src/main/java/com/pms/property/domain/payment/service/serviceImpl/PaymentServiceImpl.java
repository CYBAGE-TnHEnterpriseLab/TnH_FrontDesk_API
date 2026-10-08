package com.pms.property.domain.payment.service.serviceImpl;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.payment.dto.PaymentMethodRequest;
import com.pms.property.domain.payment.dto.PaymentMethodResponse;
import com.pms.property.domain.payment.dto.PaymentSummaryResponse;
import com.pms.property.domain.payment.entity.PaymentMethodEntity;
import com.pms.property.domain.payment.repository.PaymentMethodRepository;
import com.pms.property.domain.payment.service.PaymentService;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentServiceImpl(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getSummaryByPropertyId(String propertyId) {
        return new PaymentSummaryResponse(propertyId, paymentMethodRepository.countByPropertyId(propertyId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> listMethodsByPropertyId(String propertyId) {
        return paymentMethodRepository.findAllByPropertyId(propertyId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMethodResponse getMethodById(String propertyId, Long methodId) {
        return paymentMethodRepository.findByPropertyIdAndId(propertyId, methodId)
            .map(this::toResponse)
            .orElseThrow(() -> new NotFoundException("Payment method not found: " + methodId));
    }

    @Override
    @Transactional
    public PaymentMethodResponse createMethod(String propertyId, PaymentMethodRequest request) {
        PaymentMethodEntity entity = new PaymentMethodEntity();
        entity.setPropertyId(propertyId);
        entity.setPaymentMethod(request.paymentMethod());
        entity.setAccountMapping(request.accountMapping());
        entity.setAllowRefund(request.allowRefund());
        entity.setActive(request.active());
        return toResponse(paymentMethodRepository.save(entity));
    }

    @Override
    @Transactional
    public PaymentMethodResponse updateMethod(String propertyId, Long methodId, PaymentMethodRequest request) {
        PaymentMethodEntity entity = paymentMethodRepository.findByPropertyIdAndId(propertyId, methodId)
            .orElseThrow(() -> new NotFoundException("Payment method not found: " + methodId));
        entity.setPaymentMethod(request.paymentMethod());
        entity.setAccountMapping(request.accountMapping());
        entity.setAllowRefund(request.allowRefund());
        entity.setActive(request.active());
        return toResponse(paymentMethodRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteMethod(String propertyId, Long methodId) {
        PaymentMethodEntity entity = paymentMethodRepository.findByPropertyIdAndId(propertyId, methodId)
            .orElseThrow(() -> new NotFoundException("Payment method not found: " + methodId));
        paymentMethodRepository.delete(entity);
    }

    private PaymentMethodResponse toResponse(PaymentMethodEntity entity) {
        return new PaymentMethodResponse(
            entity.getId(),
            entity.getPropertyId(),
            entity.getPaymentMethod(),
            entity.getAccountMapping(),
            entity.getAllowRefund(),
            entity.getActive()
        );
    }
}



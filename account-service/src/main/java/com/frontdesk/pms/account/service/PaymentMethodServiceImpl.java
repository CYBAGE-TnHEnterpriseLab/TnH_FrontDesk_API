package com.frontdesk.pms.account.service;

import com.frontdesk.common.enums.LedgerType;
import com.frontdesk.pms.account.dto.PaymentMethodRequestDTO;
import com.frontdesk.pms.account.dto.PaymentMethodResponseDTO;
import com.frontdesk.pms.account.entity.ChartOfAccount;
import com.frontdesk.pms.account.entity.PaymentMethod;
import com.frontdesk.pms.account.exception.BadRequestException;
import com.frontdesk.pms.account.repository.ChartOfAccountRepository;
import com.frontdesk.pms.account.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {
    private final PaymentMethodRepository paymentMethodRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;

    @Override
    public PaymentMethodResponseDTO create(PaymentMethodRequestDTO request) {
        ChartOfAccount account = chartOfAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new BadRequestException("Account not found"));
        if (account.getLedgerType() != LedgerType.ASSET && account.getLedgerType() != LedgerType.LIABILITY) {
            throw new BadRequestException("Payment must map to Asset or Liability ledger");
        }
        PaymentMethod entity = new PaymentMethod();
        entity.setName(request.getName());
        entity.setAccountId(request.getAccountId());
        entity.setAllowRefund(request.isAllowRefund());
        entity.setActive(request.isActive());
        entity = paymentMethodRepository.save(entity);
        return toResponse(entity);
    }

    @Override
    public List<PaymentMethodResponseDTO> list() {
        return paymentMethodRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentMethodResponseDTO update(UUID id, PaymentMethodRequestDTO request) {
        PaymentMethod entity = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Payment method not found"));
        ChartOfAccount account = chartOfAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new BadRequestException("Account not found"));
        if (account.getLedgerType() != LedgerType.ASSET && account.getLedgerType() != LedgerType.LIABILITY) {
            throw new BadRequestException("Payment must map to Asset or Liability ledger");
        }
        entity.setName(request.getName());
        entity.setAccountId(request.getAccountId());
        entity.setAllowRefund(request.isAllowRefund());
        entity.setActive(request.isActive());
        entity = paymentMethodRepository.save(entity);
        return toResponse(entity);
    }

    @Override
    public void delete(UUID id) {
        paymentMethodRepository.deleteById(id);
    }

    private PaymentMethodResponseDTO toResponse(PaymentMethod entity) {
        return PaymentMethodResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .accountId(entity.getAccountId())
                .allowRefund(entity.isAllowRefund())
                .active(entity.isActive())
                .build();
    }
}

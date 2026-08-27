package com.folio.billing.service;

import com.folio.billing.dto.BillingDetailsResponse;
import com.folio.billing.dto.FolioChargeAdjustmentRequest;
import com.folio.billing.dto.FolioChargeAdjustmentResponse;
import com.folio.billing.dto.FolioBillingFilter;
import com.folio.billing.dto.FolioBillingRow;
import com.folio.billing.dto.FolioCreateRequest;
import com.folio.billing.dto.FolioCreateResponse;
import com.folio.billing.dto.FolioChargePostRequest;
import com.folio.billing.dto.FolioChargePostResponse;
import com.folio.billing.dto.FolioTransactionAmountUpdateRequest;
import com.folio.billing.dto.FolioTransactionAmountUpdateResponse;
import com.folio.billing.dto.FolioDocumentAuditEntry;
import com.folio.billing.dto.FolioDocumentContent;
import com.folio.billing.dto.FolioDocumentGenerateRequest;
import com.folio.billing.dto.FolioDocumentGenerateResponse;
import com.folio.billing.dto.FolioDocumentType;
import com.folio.billing.dto.FolioDashboardResponse;
import com.folio.billing.dto.FolioDetailsResponse;
import com.folio.billing.dto.FolioPaymentAllocationRequest;
import com.folio.billing.dto.FolioPaymentAllocationResponse;
import com.folio.billing.dto.PaymentAllocationHistoryEntry;

import java.util.List;

public interface BillingFolioService {

    List<FolioBillingRow> getFolioBilling(FolioBillingFilter filter);

    FolioCreateResponse addFolio(FolioCreateRequest request);

    BillingDetailsResponse getBillingDetails(String confirmationNumber, String roomNo, String guestName);

    FolioDashboardResponse getFolioDashboard(String confirmationNumber);
    FolioDetailsResponse getFolioDetails(String confirmationNumber);

    FolioChargePostResponse addCharge(FolioChargePostRequest request);

    FolioChargeAdjustmentResponse adjustCharge(FolioChargeAdjustmentRequest request);

    FolioTransactionAmountUpdateResponse updateTransactionAmount(FolioTransactionAmountUpdateRequest request);

    FolioPaymentAllocationResponse allocatePayment(FolioPaymentAllocationRequest request);

    List<PaymentAllocationHistoryEntry> getPaymentAllocationHistory(String confirmationNumber, String paymentReference);

    FolioDocumentGenerateResponse generateFolioDocument(FolioDocumentGenerateRequest request);

    FolioDocumentContent getFolioDocument(String documentId);

    List<FolioDocumentAuditEntry> getFolioDocumentAuditHistory(String confirmationNumber, FolioDocumentType documentType);
}


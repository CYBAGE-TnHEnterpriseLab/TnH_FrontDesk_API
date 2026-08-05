package com.folio.billing.controller;

import com.folio.billing.dto.BillingDetailsResponse;
import com.folio.billing.dto.FolioChargeAdjustmentRequest;
import com.folio.billing.dto.FolioChargeAdjustmentResponse;
import com.folio.billing.dto.FolioBillingFilter;
import com.folio.billing.dto.FolioBillingRow;
import com.folio.billing.dto.FolioChargePostRequest;
import com.folio.billing.dto.FolioChargePostResponse;
import com.folio.billing.dto.FolioDocumentAuditEntry;
import com.folio.billing.dto.FolioDocumentContent;
import com.folio.billing.dto.FolioDocumentGenerateRequest;
import com.folio.billing.dto.FolioDocumentGenerateResponse;
import com.folio.billing.dto.FolioDocumentType;
import com.folio.billing.dto.FolioDashboardResponse;
import com.folio.billing.dto.FolioPaymentAllocationRequest;
import com.folio.billing.dto.FolioPaymentAllocationResponse;
import com.folio.billing.dto.PaymentAllocationHistoryEntry;
import com.folio.billing.service.BillingFolioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/billingFolio")
public class BillingFolioController {

    private final BillingFolioService billingFolioService;

    public BillingFolioController(BillingFolioService billingFolioService) {
        this.billingFolioService = billingFolioService;
    }

    @GetMapping("/getFolioBilling")
    public ResponseEntity<List<FolioBillingRow>> getFolioBilling(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) String guestName,
            @RequestParam(required = false) String actnerCrop,
            @RequestParam(required = false) String confirmationNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate
    ) {
        FolioBillingFilter filter = new FolioBillingFilter(
                roomNumber,
                guestName,
                actnerCrop,
                confirmationNumber,
                checkInDate,
                checkOutDate
        );

        return ResponseEntity.ok(billingFolioService.getFolioBilling(filter));
    }

    @GetMapping("/getBillingDetails")
    public ResponseEntity<BillingDetailsResponse> getBillingDetails(
            @RequestParam(required = false) String confirmationNo,
            @RequestParam(required = false) String roomNo,
            @RequestParam(required = false) String guestName
    ) {
        return ResponseEntity.ok(billingFolioService.getBillingDetails(confirmationNo, roomNo, guestName));
    }

    @GetMapping("/folioDashboard")
    public ResponseEntity<FolioDashboardResponse> getFolioDashboard(
            @RequestParam(required = false) String confirmationNo
    ) {
        return ResponseEntity.ok(billingFolioService.getFolioDashboard(confirmationNo));
    }

    @PostMapping("/addCharge")
    public ResponseEntity<FolioChargePostResponse> addCharge(
            @Valid @RequestBody FolioChargePostRequest request
    ) {
        return ResponseEntity.ok(billingFolioService.addCharge(request));
    }

    @PostMapping("/adjustCharge")
    public ResponseEntity<FolioChargeAdjustmentResponse> adjustCharge(
            @Valid @RequestBody FolioChargeAdjustmentRequest request
    ) {
        return ResponseEntity.ok(billingFolioService.adjustCharge(request));
    }

    @PostMapping("/allocatePayment")
    public ResponseEntity<FolioPaymentAllocationResponse> allocatePayment(
            @Valid @RequestBody FolioPaymentAllocationRequest request
    ) {
        return ResponseEntity.ok(billingFolioService.allocatePayment(request));
    }

    @GetMapping("/paymentAllocationHistory")
    public ResponseEntity<List<PaymentAllocationHistoryEntry>> getPaymentAllocationHistory(
            @RequestParam(required = false) String confirmationNo,
            @RequestParam(required = false) String paymentReference
    ) {
        return ResponseEntity.ok(
                billingFolioService.getPaymentAllocationHistory(confirmationNo, paymentReference)
        );
    }

    @PostMapping("/generateDocument")
    public ResponseEntity<FolioDocumentGenerateResponse> generateDocument(
            @Valid @RequestBody FolioDocumentGenerateRequest request
    ) {
        return ResponseEntity.ok(billingFolioService.generateFolioDocument(request));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String documentId) {
        FolioDocumentContent document = billingFolioService.getFolioDocument(documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.fileName() + "\"")
                .body(document.content().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/documents/{documentId}/print")
    public ResponseEntity<String> printDocument(@PathVariable String documentId) {
        FolioDocumentContent document = billingFolioService.getFolioDocument(documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.fileName() + "\"")
                .body(document.content());
    }

    @GetMapping("/documentAuditHistory")
    public ResponseEntity<List<FolioDocumentAuditEntry>> getDocumentAuditHistory(
            @RequestParam(required = false) String confirmationNo,
            @RequestParam(required = false) FolioDocumentType documentType
    ) {
        return ResponseEntity.ok(
                billingFolioService.getFolioDocumentAuditHistory(confirmationNo, documentType)
        );
    }
}

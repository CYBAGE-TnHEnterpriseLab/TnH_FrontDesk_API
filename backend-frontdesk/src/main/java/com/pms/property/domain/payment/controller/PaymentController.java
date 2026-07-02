package com.pms.property.domain.payment.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.domain.payment.dto.PaymentMethodRequest;
import com.pms.property.domain.payment.dto.PaymentMethodResponse;
import com.pms.property.domain.payment.dto.PaymentSummaryResponse;
import com.pms.property.domain.payment.service.PaymentService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@GetMapping("/properties/{propertyId}/summary")
	public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getSummary(@PathVariable String propertyId) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.getSummaryByPropertyId(propertyId), "Payment summary fetched"));
	}

	@GetMapping("/properties/{propertyId}/methods")
	public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> listMethods(@PathVariable String propertyId) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.listMethodsByPropertyId(propertyId), "Payment methods fetched"));
	}

	@GetMapping("/properties/{propertyId}/methods/{methodId}")
	public ResponseEntity<ApiResponse<PaymentMethodResponse>> getMethodById(
		@PathVariable String propertyId,
		@PathVariable Long methodId
	) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.getMethodById(propertyId, methodId), "Payment method fetched"));
	}

	@PostMapping("/properties/{propertyId}/methods")
	public ResponseEntity<ApiResponse<PaymentMethodResponse>> createMethod(
		@PathVariable String propertyId,
		@RequestBody PaymentMethodRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.createMethod(propertyId, request), "Payment method created"));
	}

	@PutMapping("/properties/{propertyId}/methods/{methodId}")
	public ResponseEntity<ApiResponse<PaymentMethodResponse>> updateMethod(
		@PathVariable String propertyId,
		@PathVariable Long methodId,
		@RequestBody PaymentMethodRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.updateMethod(propertyId, methodId, request), "Payment method updated"));
	}

	@DeleteMapping("/properties/{propertyId}/methods/{methodId}")
	public ResponseEntity<ApiResponse<Void>> deleteMethod(
		@PathVariable String propertyId,
		@PathVariable Long methodId
	) {
		paymentService.deleteMethod(propertyId, methodId);
		return ResponseEntity.ok(ApiResponse.ok(null, "Payment method deleted"));
	}
}


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

	/** Fetches the payment setup summary for a published property. */
	@GetMapping("/properties/{propertyId}/summary")
	public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getSummary(@PathVariable String propertyId) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.getSummaryByPropertyId(propertyId), "Published property payment summary fetched"));
	}

	/** Fetches payment methods configured for a published property. */
	@GetMapping("/properties/{propertyId}/methods")
	public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> listMethods(@PathVariable String propertyId) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.listMethodsByPropertyId(propertyId), "Published property payment methods fetched"));
	}

	/** Fetches a payment method by id for a published property. */
	@GetMapping("/properties/{propertyId}/methods/{methodId}")
	public ResponseEntity<ApiResponse<PaymentMethodResponse>> getMethodById(
		@PathVariable String propertyId,
		@PathVariable Long methodId
	) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.getMethodById(propertyId, methodId), "Published property payment method fetched"));
	}

	/** Creates a payment method for a published property. */
	@PostMapping("/properties/{propertyId}/methods")
	public ResponseEntity<ApiResponse<PaymentMethodResponse>> createMethod(
		@PathVariable String propertyId,
		@RequestBody PaymentMethodRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.createMethod(propertyId, request), "Published property payment method created"));
	}

	/** Updates a payment method for a published property. */
	@PutMapping("/properties/{propertyId}/methods/{methodId}")
	public ResponseEntity<ApiResponse<PaymentMethodResponse>> updateMethod(
		@PathVariable String propertyId,
		@PathVariable Long methodId,
		@RequestBody PaymentMethodRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(paymentService.updateMethod(propertyId, methodId, request), "Published property payment method updated"));
	}

	/** Deletes a payment method from a published property. */
	@DeleteMapping("/properties/{propertyId}/methods/{methodId}")
	public ResponseEntity<ApiResponse<Void>> deleteMethod(
		@PathVariable String propertyId,
		@PathVariable Long methodId
	) {
		paymentService.deleteMethod(propertyId, methodId);
		return ResponseEntity.ok(ApiResponse.ok(null, "Published property payment method deleted"));
	}
}


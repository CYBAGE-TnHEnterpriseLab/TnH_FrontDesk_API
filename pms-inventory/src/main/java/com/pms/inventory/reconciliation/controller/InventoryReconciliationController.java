package com.pms.inventory.reconciliation.controller;

import com.pms.inventory.reconciliation.dto.InventoryReconciliationRequest;
import com.pms.inventory.reconciliation.service.InventoryReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory/reconciliation")
public class InventoryReconciliationController {

	private final InventoryReconciliationService reconciliationService;

	public InventoryReconciliationController(InventoryReconciliationService reconciliationService) {
		this.reconciliationService = reconciliationService;
	}

	@PostMapping
	@Operation(summary = "Reconcile inventory from external property master data")
	public ResponseEntity<Map<String, Integer>> reconcile(@Valid @RequestBody InventoryReconciliationRequest request) {
		int affectedRows = reconciliationService.reconcile(request);
		return ResponseEntity.ok(Map.of("affectedRows", affectedRows));
	}
}


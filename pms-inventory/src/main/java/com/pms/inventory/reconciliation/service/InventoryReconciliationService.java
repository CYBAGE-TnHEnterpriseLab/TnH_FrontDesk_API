package com.pms.inventory.reconciliation.service;

import com.pms.inventory.reconciliation.dto.InventoryReconciliationRequest;
import org.springframework.stereotype.Service;

@Service
public class InventoryReconciliationService {

	private final com.pms.inventory.inventory.service.InventoryReconciliationService reconciliationService;

	public InventoryReconciliationService(
			com.pms.inventory.inventory.service.InventoryReconciliationService reconciliationService
	) {
		this.reconciliationService = reconciliationService;
	}

	public int reconcile(InventoryReconciliationRequest request) {
		return reconciliationService.reconcile(request);
	}
}


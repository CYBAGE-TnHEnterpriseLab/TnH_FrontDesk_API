package Policy_Management.Policy.controller;
import Policy_Management.Policy.dto.APIResponse;
import Policy_Management.Policy.dto.PolicyListResponse;
import Policy_Management.Policy.dto.PolicyDto;
import Policy_Management.Policy.dto.Status;
import com.pms.security.jwt.CurrentUserProvider;
import Policy_Management.Policy.service.PolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyController.class);
    private final PolicyService service;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public PolicyController(PolicyService service, CurrentUserProvider currentUserProvider) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
    }
    
    @PostMapping(value = "/createPolicy")
    public ResponseEntity<APIResponse<PolicyDto>> create(@RequestBody PolicyDto dto) {
        dto.setCreatedBy(currentUserProvider.getCurrentUsername());
        LOGGER.info("POST /api/v1/policies/createPolicy request: {}", dto);
        PolicyDto created = service.createPolicy(dto);
        APIResponse<PolicyDto> response = new APIResponse<>("success", "Policy created successfully", created);
        LOGGER.info("POST /api/v1/policies/createPolicy response: {}", response);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping (value = "/getAllPolicies")
    public ResponseEntity<APIResponse<PolicyListResponse>> getAll(@RequestParam(value = "status", required = false) Status status) {
        LOGGER.info("GET /api/v1/policies/getAllPolicies request: status={}", status);
        APIResponse<PolicyListResponse> response = new APIResponse<>("success", "Policies retrieved successfully", service.getAllPolicies(status));
        LOGGER.info("GET /api/v1/policies/getAllPolicies response: {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/getPoliciesById/{id}")
    public ResponseEntity<APIResponse<PolicyDto>> getById(@PathVariable Long id) {
        LOGGER.info("GET /api/v1/policies/getPoliciesById/{} request", id);
        APIResponse<PolicyDto> response = new APIResponse<>("success", "Policy retrieved successfully", service.getPolicyById(id));
        LOGGER.info("GET /api/v1/policies/getPoliciesById/{} response: {}", id, response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/getPoliciesByPropertyId/{propertyId}")
    public ResponseEntity<APIResponse<PolicyListResponse>> getByPropertyId(@PathVariable String propertyId) {
        LOGGER.info("GET /api/v1/policies/getPoliciesByPropertyId/{} request", propertyId);
        APIResponse<PolicyListResponse> response = new APIResponse<>("success", 
        "Policies retrieved successfully", service.getAllPoliciesByPropertyId(propertyId));
        LOGGER.info("GET /api/v1/policies/getPoliciesByPropertyId/{} response: {}", propertyId, response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping(value = "/updatePolicy/{id}")
    public ResponseEntity<APIResponse<PolicyDto>> update(@PathVariable Long id, @RequestBody PolicyDto dto) {
        LOGGER.info("PUT /api/v1/policies/updatePolicy/{} request: {}", id, dto);
        APIResponse<PolicyDto> response = new APIResponse<>("success", "Policy updated successfully", service.updatePolicy(id, dto));
        LOGGER.info("PUT /api/v1/policies/updatePolicy/{} response: {}", id, response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping(value = "/mapPolicyToProperty/{policyId}/{propertyId}")
    public ResponseEntity<APIResponse<PolicyDto>> mapPolicyToProperty(@PathVariable Long policyId, @PathVariable String propertyId) {
        LOGGER.info("PUT /api/v1/policies/mapPolicyToProperty/{}/{} request", policyId, propertyId);
        PolicyDto mapped = service.mapPolicyToProperty(policyId, propertyId);
        APIResponse<PolicyDto> response = new APIResponse<>("success", "Policy mapped to property successfully", mapped);
        LOGGER.info("PUT /api/v1/policies/mapPolicyToProperty/{}/{} response: {}", policyId, propertyId, response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping(value = "/unmapPolicyFromProperty/{policyId}")
    public ResponseEntity<APIResponse<PolicyDto>> unmapPolicyFromProperty(@PathVariable Long policyId) {
        LOGGER.info("PUT /api/v1/policies/unmapPolicyFromProperty/{} request", policyId);
        PolicyDto unmapped = service.unmapPolicyFromProperty(policyId);
        APIResponse<PolicyDto> response = new APIResponse<>("success", "Policy unmapped from property successfully", unmapped);
        LOGGER.info("PUT /api/v1/policies/unmapPolicyFromProperty/{} response: {}", policyId, response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //@PutMapping(value = "/updatePolicyByPropertyId/{PropertyId}")
     
    @DeleteMapping(value = "/deletePolicy/{id}")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable Long id) {
        LOGGER.info("DELETE /api/v1/policies/deletePolicy/{} request", id);
        service.deletePolicy(id);
        APIResponse<Void> response = new APIResponse<>("success", "Policy deleted successfully", null);
        LOGGER.info("DELETE /api/v1/policies/deletePolicy/{} response: {}", id, response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

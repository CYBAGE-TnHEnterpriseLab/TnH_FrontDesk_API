package Policy_Management.Policy.service;

import Policy_Management.Policy.dto.PolicyDto;
import Policy_Management.Policy.dto.PolicyListResponse;
import Policy_Management.Policy.dto.Status;




public interface PolicyService {

    PolicyDto createPolicy(PolicyDto dto);

    PolicyListResponse getAllPolicies();

    PolicyListResponse getAllPolicies(Status status);

    PolicyDto getPolicyById(Long id);

    PolicyDto updatePolicy(Long id, PolicyDto dto);

    void deletePolicy(Long id);



}

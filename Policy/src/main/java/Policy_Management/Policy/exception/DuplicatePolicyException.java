package Policy_Management.Policy.exception;

import Policy_Management.Policy.dto.PolicyDto;

public class DuplicatePolicyException  extends RuntimeException {
    
    public DuplicatePolicyException(PolicyDto dto) {
        super("Policy with Code " + dto.getPolicyCode() + " already exists.");
    }
}
    
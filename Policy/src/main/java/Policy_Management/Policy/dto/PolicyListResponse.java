package Policy_Management.Policy.dto;

import java.util.List;

public class PolicyListResponse {

    private List<PolicyDto> policies;
    private int totalPolicies;
    private int activePolicies;
    private int draftPolicies;
    private int inactivePolicies;

    public List<PolicyDto> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyDto> policies) {
        this.policies = policies;
    }

    public int getTotalPolicies() {
        return totalPolicies;
    }

    public void setTotalPolicies(int totalPolicies) {
        this.totalPolicies = totalPolicies;
    }

    public int getActivePolicies() {
        return activePolicies;
    }

    public void setActivePolicies(int activePolicies) {
        this.activePolicies = activePolicies;
    }

    public int getDraftPolicies() {
        return draftPolicies;
    }

    public void setDraftPolicies(int draftPolicies) {
        this.draftPolicies = draftPolicies;
    }

    public int getInactivePolicies() {
        return inactivePolicies;
    }

    public void setInactivePolicies(int inactivePolicies) {
        this.inactivePolicies = inactivePolicies;
    }

    @Override
    public String toString() {
        return "PolicyListResponse{" +
                "policies=" + policies +
                ", totalPolicies=" + totalPolicies +
                ", activePolicies=" + activePolicies +
                ", draftPolicies=" + draftPolicies +
                ", inactivePolicies=" + inactivePolicies +
                '}';
    }
}

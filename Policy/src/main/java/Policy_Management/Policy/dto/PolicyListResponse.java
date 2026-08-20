package Policy_Management.Policy.dto;
import Policy_Management.Policy.dto.Status;
import java.util.List;

public class PolicyListResponse {

    private List<PolicyDto> policies;
    private int totalPolicies;
    private int activePolicies;
    private int draftPolicies;
    private int inactivePolicies;

    public PolicyListResponse(List<PolicyDto> list) {
        //TODO Auto-generated constructor stub
        this.policies = list;
        this.totalPolicies = list.size();
        this.activePolicies = (int) list.stream().filter(p -> p.getStatus() == Status.ACTIVE).count();
        this.draftPolicies = (int) list.stream().filter(p -> p.getStatus() == Status.DRAFT).count();
        this.inactivePolicies = (int) list.stream().filter(p -> p.getStatus() == Status.INACTIVE).count();
    }

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

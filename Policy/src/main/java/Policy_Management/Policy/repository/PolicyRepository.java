package Policy_Management.Policy.repository;

import Policy_Management.Policy.dto.Status;
import Policy_Management.Policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    boolean existsByPolicyCode(String policyCode);

    List<Policy> findAllByStatus(Status status);

    int countByStatus(Status status);

    @Modifying
    @Query("update Policy p set p.policyCount = :count where p.status = :status")
    int updatePolicyCountByStatus(Status status, int count);
}

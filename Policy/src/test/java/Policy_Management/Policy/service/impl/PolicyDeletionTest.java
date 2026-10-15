package Policy_Management.Policy.service.impl;

import Policy_Management.Policy.config.RateManagementClient;
import Policy_Management.Policy.dto.Status;
import Policy_Management.Policy.entity.Policy;
import Policy_Management.Policy.repository.PolicyRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyDeletionTest {

    @Mock
    private PolicyRepository repository;

    @Mock
    private RateManagementClient rateManagementClient;

    @InjectMocks
    private PolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service.rateManagementClient = rateManagementClient;
    }

    @Test
    void shouldUnlinkPolicyBeforeDeletingIt() {
        Policy policy = new Policy();
        policy.setStatus(Status.DRAFT);
        when(repository.findById(7L)).thenReturn(Optional.of(policy));

        service.deletePolicy(7L);

        var deletionOrder = inOrder(rateManagementClient, repository);
        deletionOrder.verify(rateManagementClient).deletePolicyUpdateInRatePlan("7");
        deletionOrder.verify(repository).deleteById(7L);
    }

    @Test
    void shouldKeepPolicyWhenRateManagementCleanupFails() {
        when(repository.findById(7L)).thenReturn(Optional.of(new Policy()));
        doThrow(new IllegalStateException("Rate management unavailable"))
                .when(rateManagementClient).deletePolicyUpdateInRatePlan("7");

        assertThrows(IllegalStateException.class, () -> service.deletePolicy(7L));

        verify(repository, never()).deleteById(7L);
    }
}
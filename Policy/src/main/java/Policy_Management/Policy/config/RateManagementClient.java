package Policy_Management.Policy.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.stereotype.Component;
@Component
public class RateManagementClient {


    private final WebClient rateManagementWebClient;
    private final HttpServletRequest request;

    public RateManagementClient(@Qualifier("rateManagementWebClient") WebClient rateManagementWebClient, HttpServletRequest request) {
        this.rateManagementWebClient = rateManagementWebClient;
        this.request = request;
    }

    public void deletePolicyUpdateInRatePlan(String policyId) {
        rateManagementWebClient.delete()
                .uri("/api/rate-plans/delete/policy-from-rate-plan/property/{policyId}", policyId)
                .headers(headers -> {
                    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
                    if (auth != null && !auth.isBlank()) {
                        headers.set(HttpHeaders.AUTHORIZATION, auth);
                    }
                })
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
    
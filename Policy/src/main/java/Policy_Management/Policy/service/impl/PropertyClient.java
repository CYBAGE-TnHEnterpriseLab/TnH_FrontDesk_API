package Policy_Management.Policy.service.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import Policy_Management.Policy.dto.APIResponse;
import Policy_Management.Policy.dto.PropertyDto;
import Policy_Management.Policy.exception.PolicyValidationException;
import jakarta.servlet.http.HttpServletRequest;



@Service
public class PropertyClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyClient.class);
    
     @Autowired
    private RestTemplate restTemplate;

    @Autowired
     private HttpServletRequest request;

     @Value("${property.service.url}")
     private String propertyServiceUrl;

     public PropertyDto getPropertyById(String propertyId){
        String token = request.getHeader("Authorization");

           HttpHeaders headers = new HttpHeaders();
           if (token != null && !token.isBlank()) {
                headers.set("Authorization", token);
           }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

      ParameterizedTypeReference<APIResponse<PropertyDto>> type =
        new ParameterizedTypeReference<>() {};

      try {
          ResponseEntity<APIResponse<PropertyDto>> response =
            restTemplate.exchange(
                    propertyServiceUrl,
                    HttpMethod.GET,
                    entity,
                    type,
                    propertyId);

          APIResponse<PropertyDto> body = response.getBody();
                if (body == null || body.getData() == null) {
                  throw propertyApiValidation("Property API returned empty response for propertyId=" + propertyId);
                }
                return body.getData();
              } catch (HttpStatusCodeException ex) {
                HttpStatusCode statusCode = ex.getStatusCode();
                LOGGER.warn("Property API call failed: status={} propertyId={} body={}", statusCode, propertyId, ex.getResponseBodyAsString());
                throw propertyApiValidation("Property API lookup failed with status " + statusCode.value() + " for propertyId=" + propertyId);
              } catch (Exception ex) {
                LOGGER.warn("Property API call error for propertyId={}: {}", propertyId, ex.getMessage(), ex);
                throw propertyApiValidation("Property API lookup failed for propertyId=" + propertyId + ": " + ex.getMessage());
      }

     }

             private PolicyValidationException propertyApiValidation(String message) {
              java.util.Map<String, String> errors = new java.util.HashMap<>();
              errors.put("propertyId", message);
              return new PolicyValidationException(errors);
             }
}

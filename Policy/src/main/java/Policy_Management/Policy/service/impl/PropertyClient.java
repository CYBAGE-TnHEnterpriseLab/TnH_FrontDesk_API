package Policy_Management.Policy.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import Policy_Management.Policy.dto.APIResponse;
import Policy_Management.Policy.dto.PropertyDto;
import jakarta.servlet.http.HttpServletRequest;


@Service
public class PropertyClient {
    
     @Autowired
    private RestTemplate restTemplate;

    @Autowired
     private HttpServletRequest request;

     public PropertyDto getProperty(Long propertyId){
        String token = request.getHeader("Authorization");
        String url = "http://localhost:8082/api/property/getPublishedProperty/" + propertyId;

           HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

      ParameterizedTypeReference<APIResponse<PropertyDto>> type =
        new ParameterizedTypeReference<>() {};

      ResponseEntity<APIResponse<PropertyDto>> response =
        restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                type);

      PropertyDto property = response.getBody().getData();
      return property;
     }
}

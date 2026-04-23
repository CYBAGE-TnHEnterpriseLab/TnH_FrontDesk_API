package com.frontdesk.pms.service;

import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import java.util.List;
import java.util.stream.Collectors;

public interface PropertyService {

    PropertyResponseDTO createProperty(PropertyRequestDTO requestDTO);

    List<PropertyResponseDTO> getAllProperties();

    PropertyResponseDTO getPropertyById(Long id);


}
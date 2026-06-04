package com.hotel.pms.frontdesk.guestlisting.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PagedResponse<T> {
    String propertyId;
    LocalDate businessDate;
    FilterOptionsDto filterOptions;
    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;
    String sortBy;
    String sortDir;
}

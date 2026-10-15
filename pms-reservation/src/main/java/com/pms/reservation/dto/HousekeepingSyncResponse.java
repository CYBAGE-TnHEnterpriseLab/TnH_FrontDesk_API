package com.pms.reservation.dto;

public record HousekeepingSyncResponse(int processed, int updated, int skipped, int failed) {}

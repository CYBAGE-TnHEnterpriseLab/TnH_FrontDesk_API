package com.pms.reservation.repository;

import java.math.BigDecimal;

public interface DailyRevenueProjection {

    BigDecimal getRoomRevenue();

    Long getRoomsSold();

    Long getIndividualBookings();

    Long getGroupBookings();
}
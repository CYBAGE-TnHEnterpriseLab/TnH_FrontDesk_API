package com.hotel.pms.frontdesk.guestlisting.integration.mock;

import com.hotel.pms.frontdesk.guestlisting.dto.ReservationArrivalDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dummy")
@RequestMapping("/internal/mock/reservations")
public class MockReservationController {

    @GetMapping("/arrivals")
    public List<ReservationArrivalDto> getArrivals(
            @RequestParam String propertyId,
            @RequestParam LocalDate businessDate
    ) {
                String normalizedPropertyId = propertyId.trim().toUpperCase(Locale.ROOT);
                int roomOffset = switch (normalizedPropertyId) {
                        case "PROP002" -> 100;
                        case "PROP003" -> 200;
                        default -> 0;
                };
                List<String> cityPalette = switch (normalizedPropertyId) {
                        case "PROP002" -> List.of("Hyderabad", "Noida", "Kolkata");
                        case "PROP003" -> List.of("Indore", "Surat", "Nagpur");
                        default -> List.of("Mumbai", "Pune", "Ahmedabad");
                };

                List<ReservationArrivalDto> baseArrivals = List.of(
                build("DNM", "Mr.", "John", "Smith", "305", "Guaranteed", "Mumbai", "BAR",
                        businessDate, businessDate.plusDays(3), null, "Clean", "CORP001", "Deluxe King",
                        "CNF458721", "ABC Travels", "Y", 3, new BigDecimal("1250.50"), "Gold Member"),
                build("", "Ms.", "Aisha", "Khan", "412", "Non-Guaranteed", "Pune", "WKND",
                        businessDate, businessDate.plusDays(2), 2, "Dirty", "CORP009", "Executive Twin",
                        "CNF458722", "Skyline Holidays", "", 4, new BigDecimal("540.00"), "Silver Member"),
                build("", "Mr.", "Ravi", "Patel", "210", "Guaranteed", "Ahmedabad", "BAR",
                        businessDate, businessDate.plusDays(1), 1, "Inspected", "CORP111", "Superior Queen",
                        "CNF458723", "Global Corp", "", 2, new BigDecimal("0.00"), "Platinum Member"),
                build("", "Mrs.", "Neha", "Sharma", "118", "Guaranteed", "Delhi", "CORP",
                        businessDate, businessDate.plusDays(4), 4, "Clean", "CORP550", "Deluxe Twin",
                        "CNF458724", "Zenith Logistics", "", 1, new BigDecimal("2100.75"), "Gold Member"),
                build("", "Mr.", "Arjun", "Mehta", "524", "Non-Guaranteed", "Bengaluru", "BAR",
                        businessDate, businessDate.plusDays(2), 2, "Clean", "CORP320", "Executive King",
                        "CNF458725", "Orbit DMC", "Y", 5, new BigDecimal("875.25"), "Silver Member"),
                build("DNM", "Dr.", "Priya", "Iyer", "333", "Guaranteed", "Chennai", "MED",
                        businessDate, businessDate.plusDays(5), 5, "Out of Order", "CORP777", "Premier Suite",
                        "CNF458726", "Medicon Events", "", 3, new BigDecimal("4300.00"), "Platinum Member"),
                build("", "Mr.", "Karan", "Verma", "145", "Guaranteed", "Jaipur", "BAR",
                        businessDate, businessDate.plusDays(1), 1, "Clean", "CORP210", "Standard Queen",
                        "CNF458727", "Royal Tours", "", 1, new BigDecimal("320.00"), "Blue Member"),
                build("", "Ms.", "Sara", "D'Souza", "606", "Non-Guaranteed", "Goa", "LEISURE",
                        businessDate, businessDate.plusDays(3), 3, "Dirty", "CORP980", "Sea View King",
                        "CNF458728", "Sunbird Vacations", "Y", 6, new BigDecimal("980.40"), "Gold Member"),
                build("", "Mr.", "Imran", "Ali", "287", "Guaranteed", "Lucknow", "BAR",
                        businessDate, businessDate.plusDays(2), 2, "Inspected", "CORP430", "Superior Twin",
                        "CNF458729", "Northline Travels", "", 2, new BigDecimal("150.00"), "Silver Member"),
                build("", "Mrs.", "Pooja", "Nair", "451", "Guaranteed", "Kochi", "FAMILY",
                        businessDate, businessDate.plusDays(4), 4, "Clean", "CORP120", "Family Suite",
                        "CNF458730", "Coastal Holidays", "Y", 4, new BigDecimal("2650.10"), "Gold Member")
                );

                return IntStream.range(0, baseArrivals.size())
                                .mapToObj(index -> applyPropertyFlavor(baseArrivals.get(index), propertyId, index, roomOffset, cityPalette))
                                .toList();
    }

        @GetMapping("/departures")
        public List<ReservationArrivalDto> getDepartures(
                        @RequestParam String propertyId,
                        @RequestParam LocalDate businessDate
        ) {
                return getArrivals(propertyId, businessDate).stream()
                                .map(arrival -> {
                                        int nights = arrival.getRoomNights() != null ? arrival.getRoomNights() : 1;
                                        arrival.setCheckOutDate(businessDate);
                                        arrival.setCheckInDate(businessDate.minusDays(Math.max(1, nights)));
                                        return arrival;
                                })
                                .toList();
        }

        private ReservationArrivalDto applyPropertyFlavor(
                        ReservationArrivalDto dto,
                        String propertyId,
                        int index,
                        int roomOffset,
                        List<String> cityPalette
        ) {
                LocalDate baseCheckInDate = LocalDate.of(2026, 6, 1).plusDays(index % 5L);
                int nights = dto.getRoomNights() != null
                        ? dto.getRoomNights()
                        : Math.max(1, (int) ChronoUnit.DAYS.between(dto.getCheckInDate(), dto.getCheckOutDate()));

                dto.setRoomNo(String.valueOf(parseRoomNo(dto.getRoomNo()) + roomOffset));
                dto.setCity(cityPalette.get(index % cityPalette.size()));
                dto.setCompany(dto.getCompany() + " - " + propertyId.toUpperCase(Locale.ROOT));
                dto.setCorporateCode(dto.getCorporateCode() + "-" + propertyId.toUpperCase(Locale.ROOT));
                dto.setConfirmationNumber(propertyId.toUpperCase(Locale.ROOT) + "-" + dto.getConfirmationNumber());
                dto.setCheckInDate(baseCheckInDate);
                dto.setCheckOutDate(baseCheckInDate.plusDays(nights));
                if (dto.getFloor() != null) {
                        dto.setFloor(Math.min(9, dto.getFloor() + (roomOffset / 100)));
                }
                return dto;
        }

        private int parseRoomNo(String roomNo) {
                try {
                        return Integer.parseInt(roomNo);
                } catch (NumberFormatException ex) {
                        return 0;
                }
        }

    private ReservationArrivalDto build(
            String status,
            String salutation,
            String firstName,
            String lastName,
            String roomNo,
            String reservationType,
            String city,
            String rateCode,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomNights,
            String roomStatus,
            String corporateCode,
            String roomType,
            String confirmationNumber,
            String company,
            String sharingStatus,
            Integer floor,
            BigDecimal balance,
            String loyaltyMembershipStatus
    ) {
        ReservationArrivalDto dto = new ReservationArrivalDto();
        dto.setStatus(status);
        dto.setSalutation(salutation);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setRoomNo(roomNo);
        dto.setReservationType(reservationType);
        dto.setCity(city);
        dto.setRateCode(rateCode);
        dto.setCheckInDate(checkInDate);
        dto.setCheckOutDate(checkOutDate);
        dto.setRoomNights(roomNights);
        dto.setRoomStatus(roomStatus);
        dto.setCorporateCode(corporateCode);
        dto.setRoomType(roomType);
        dto.setConfirmationNumber(confirmationNumber);
        dto.setCompany(company);
        dto.setSharingStatus(sharingStatus);
        dto.setFloor(floor);
                dto.setBalance(balance);
        dto.setLoyaltyMembershipStatus(loyaltyMembershipStatus);
        return dto;
    }
}

package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.dto.ReservationGuestResponseDto;
import com.pms.reservation.entity.ReservationGuest;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationGuestRepository;
import com.pms.reservation.service.ResolvedReservationGuest;
import com.pms.reservation.service.ReservationGuestService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReservationGuestServiceImpl implements ReservationGuestService {

    private final ReservationGuestRepository reservationGuestRepository;
    private final ReservationBookingRepository reservationBookingRepository;
    private final GuestServiceClient guestServiceClient;
    private final TransactionTemplate transactionTemplate;

    public ReservationGuestServiceImpl(
            ReservationGuestRepository reservationGuestRepository,
            ReservationBookingRepository reservationBookingRepository,
            GuestServiceClient guestServiceClient,
            PlatformTransactionManager transactionManager
    ) {
        this.reservationGuestRepository = reservationGuestRepository;
        this.reservationBookingRepository = reservationBookingRepository;
        this.guestServiceClient = guestServiceClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void assignResolvedGuests(Long bookingId, List<ResolvedReservationGuest> guests) {
        if (guests == null || guests.isEmpty()) {
            throw new BadRequestException("At least one resolved reservation guest is required");
        }
        Boolean assigned = transactionTemplate.execute(status -> {
            verifyBookingExists(bookingId);
            List<ReservationGuest> relationships = new ArrayList<>(guests.size());
            for (ResolvedReservationGuest guest : guests) {
                relationships.add(ReservationGuest.builder()
                        .bookingId(bookingId)
                        .guestProfileId(guest.guestProfileId())
                        .isPrimary(guest.isPrimary())
                        .build());
            }
            reservationGuestRepository.saveAllAndFlush(relationships);
            return Boolean.TRUE;
        });
        if (!Boolean.TRUE.equals(assigned)) {
            throw new IllegalStateException("Guest assignment transaction did not complete");
        }
    }

    @Override
    public ReservationGuestResponseDto assignGuestToBooking(Long bookingId, Long guestProfileId, boolean primary) {
        String propertyId = findBookingPropertyId(bookingId);
        GuestProfileResponse guestProfile = findGuestProfile(guestProfileId, propertyId);

        ReservationGuestResponseDto response = transactionTemplate.execute(status ->
                assignWithinTransaction(bookingId, guestProfileId, primary, guestProfile));
        if (response == null) {
            throw new IllegalStateException("Guest assignment transaction returned no result");
        }
        return response;
    }

    @Override
    public List<ReservationGuestResponseDto> getGuestsForBooking(Long bookingId) {
        String propertyId = findBookingPropertyId(bookingId);
        return reservationGuestRepository.findByBookingId(bookingId).stream()
                .map(relationship -> toResponse(
                        relationship,
                        findGuestProfile(relationship.getGuestProfileId(), propertyId)))
                .toList();
    }

    @Override
    public ReservationGuestResponseDto getPrimaryGuest(Long bookingId) {
        String propertyId = findBookingPropertyId(bookingId);
        ReservationGuest primaryGuest = reservationGuestRepository.findByBookingIdAndIsPrimaryTrue(bookingId)
                .orElseThrow(() -> new BadRequestException("Primary guest not found for booking"));
        return toResponse(primaryGuest, findGuestProfile(primaryGuest.getGuestProfileId(), propertyId));
    }

    @Override
    public ReservationGuestResponseDto makePrimaryGuest(Long bookingId, Long reservationGuestId) {
        ReservationGuest selected = transactionTemplate.execute(status -> {
            verifyBookingExists(bookingId);
            ReservationGuest relationship = reservationGuestRepository
                    .findByIdAndBookingId(reservationGuestId, bookingId)
                    .orElseThrow(() -> new BadRequestException(
                            "Reservation guest does not belong to the specified booking"));

            if (!Boolean.TRUE.equals(relationship.getIsPrimary())) {
                clearCurrentPrimary(bookingId, relationship.getId());
                relationship.setIsPrimary(true);
                relationship = reservationGuestRepository.saveAndFlush(relationship);
            }
            return relationship;
        });
        if (selected == null) {
            throw new IllegalStateException("Make-primary transaction returned no result");
        }

        return toResponse(selected, findGuestProfile(
                selected.getGuestProfileId(),
                findBookingPropertyId(bookingId)));
    }

    @Override
    public void removeGuestFromBooking(Long bookingId, Long guestProfileId) {
        Boolean removed = transactionTemplate.execute(status -> {
            verifyBookingExists(bookingId);
            ReservationGuest relationship = reservationGuestRepository
                    .findByBookingIdAndGuestProfileId(bookingId, guestProfileId)
                    .orElseThrow(() -> new BadRequestException("Guest is not assigned to this booking"));
            if (Boolean.TRUE.equals(relationship.getIsPrimary())) {
                throw new BadRequestException(
                        "Cannot remove the primary guest until another guest is assigned as primary");
            }
            reservationGuestRepository.delete(relationship);
            return Boolean.TRUE;
        });
        if (!Boolean.TRUE.equals(removed)) {
            throw new IllegalStateException("Guest removal transaction did not complete");
        }
    }

    private ReservationGuestResponseDto assignWithinTransaction(
            Long bookingId,
            Long guestProfileId,
            boolean primary,
            GuestProfileResponse guestProfile
    ) {
        Optional<ReservationGuest> existing = reservationGuestRepository
                .findByBookingIdAndGuestProfileId(bookingId, guestProfileId);

        ReservationGuest relationship;
        if (existing.isPresent()) {
            relationship = existing.get();
            if (primary) {
                makePrimary(relationship, bookingId);
            }
        } else {
            relationship = ReservationGuest.builder()
                    .bookingId(bookingId)
                    .guestProfileId(guestProfileId)
                    .isPrimary(primary)
                    .build();
            if (primary) {
                clearCurrentPrimary(bookingId, null);
            }
            relationship = reservationGuestRepository.saveAndFlush(relationship);
        }
        return toResponse(relationship, guestProfile);
    }

    private void makePrimary(ReservationGuest relationship, Long bookingId) {
        if (Boolean.TRUE.equals(relationship.getIsPrimary())) {
            return;
        }
        clearCurrentPrimary(bookingId, relationship.getId());
        relationship.setIsPrimary(true);
        reservationGuestRepository.saveAndFlush(relationship);
    }

    private void clearCurrentPrimary(Long bookingId, Long exceptRelationshipId) {
        reservationGuestRepository.findByBookingIdAndIsPrimaryTrue(bookingId)
                .filter(current -> !current.getId().equals(exceptRelationshipId))
                .ifPresent(current -> {
                    current.setIsPrimary(false);
                    reservationGuestRepository.saveAndFlush(current);
                });
    }

    private String findBookingPropertyId(Long bookingId) {
        return reservationBookingRepository.findById(bookingId)
                .map(booking -> booking.getPropertyId())
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
    }

    private void verifyBookingExists(Long bookingId) {
        if (!reservationBookingRepository.existsById(bookingId)) {
            throw new BadRequestException("Reservation booking not found");
        }
    }

    private GuestProfileResponse findGuestProfile(Long guestProfileId, String propertyId) {
        return guestServiceClient.getGuestById(guestProfileId, propertyId)
                .orElseThrow(() -> new BadRequestException("Guest profile not found"));
    }

    private ReservationGuestResponseDto toResponse(
            ReservationGuest relationship,
            GuestProfileResponse guestProfile
    ) {
        return ReservationGuestResponseDto.builder()
                .id(relationship.getId())
                .bookingId(relationship.getBookingId())
                .guestProfileId(relationship.getGuestProfileId())
                .isPrimary(relationship.getIsPrimary())
                .guestProfile(guestProfile)
                .build();
    }
}

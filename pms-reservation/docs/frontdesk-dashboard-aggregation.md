# Frontdesk Dashboard Aggregation

Endpoint:

- `GET /api/v1/frontdesk/dashboard?propertyId={uuid}&businessDate={yyyy-MM-dd}`

This API aggregates data from:

- Housekeeping: `/api/v1/housekeeping/dashboard`, `/api/v1/housekeeping/rooms`
- Inventory: `/api/v1/inventory/daily`
- Property: `/api/rooms/properties/{propertyId}/room-outlet-types`
- Rate: `/api/rate-plans/property/{propertyId}`
- Reservation flow: `/api/v1/guest-listing/list?view=arrivals|departures`

It returns partial data with `sources` status map when a source is unavailable.


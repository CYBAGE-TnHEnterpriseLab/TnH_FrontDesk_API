CREATE INDEX IF NOT EXISTS idx_guest_profiles_property_mobile
    ON guestdb.guest_profiles(property_id, mobile_number);

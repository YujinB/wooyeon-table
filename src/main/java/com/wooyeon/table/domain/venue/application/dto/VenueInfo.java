package com.wooyeon.table.domain.venue.application.dto;

import com.wooyeon.table.domain.venue.domain.model.Venue;

public record VenueInfo(Long venueId, String name, String region, String address, String mapUrl,
                        String mealBudget, String priceGuide, int tablesPerSlot, boolean autoBookable,
                        boolean active) {

    public static VenueInfo from(Venue venue) {
        return new VenueInfo(venue.getId(), venue.getName(), venue.getRegion().getLabel(), venue.getAddress(),
                venue.getMapUrl(), venue.getMealBudget().getLabel(), venue.getPriceGuide(), venue.getTablesPerSlot(),
                venue.isAutoBookable(), venue.isActive());
    }
}

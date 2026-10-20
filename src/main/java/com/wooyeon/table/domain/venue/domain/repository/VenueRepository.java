package com.wooyeon.table.domain.venue.domain.repository;

import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.venue.domain.model.Venue;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VenueRepository {

    Venue save(Venue venue);

    long count();

    Optional<Venue> findById(Long id);

    List<Venue> findAll();

    List<Venue> findAllByIdIn(Collection<Long> ids);

    /** 그 지역의 운영 중인 파트너 식당 */
    List<Venue> findActiveByRegion(Region region);
}

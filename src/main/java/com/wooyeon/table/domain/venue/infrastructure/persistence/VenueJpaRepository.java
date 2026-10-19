package com.wooyeon.table.domain.venue.infrastructure.persistence;

import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.venue.domain.model.Venue;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueJpaRepository extends JpaRepository<Venue, Long> {

    List<Venue> findAllByRegionAndActiveTrueOrderByIdAsc(Region region);

    List<Venue> findAllByIdIn(Collection<Long> ids);

    List<Venue> findAllByOrderByRegionAscIdAsc();
}

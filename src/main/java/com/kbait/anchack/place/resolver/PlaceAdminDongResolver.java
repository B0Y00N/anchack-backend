package com.kbait.anchack.place.resolver;

import com.kbait.anchack.place.dto.external.ExternalPlace;

import java.util.Optional;

public interface PlaceAdminDongResolver {

    Optional<Long> resolveAdminDongId(ExternalPlace externalPlace);
}

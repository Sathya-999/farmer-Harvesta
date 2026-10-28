package com.agriplatform.plot.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for a Plot resource.
 *
 * <h2>GPS / Location</h2>
 * <p>This DTO does NOT expose {@code Village.latitude} or {@code Village.longitude}.
 * Village coordinates are geography reference data and MUST NOT be interpreted as
 * the physical GPS location of the plot. Plot GPS is deferred to Beta V2.
 *
 * <h2>Version Token</h2>
 * <p>{@code version} is an opaque ISO-8601 microsecond-precision string derived from
 * {@code updated_at}. Clients must echo it verbatim as {@code expectedVersion} in
 * update requests. Do NOT parse it as a date — treat as an opaque token.
 *
 * @param id              Plot UUID
 * @param farmerId        Owner farmer UUID (from JWT sub)
 * @param villageId       Village UUID where plot lies
 * @param villageName     Village name
 * @param mandalName      Parent mandal name
 * @param districtName    Parent district name
 * @param stateName       Parent state name
 * @param label           Optional human-readable label
 * @param totalAreaAcres  Area in acres (nullable)
 * @param isActive        Whether plot is currently active
 * @param version         Opaque ISO-8601 microsecond concurrency token
 * @param createdAt       Creation timestamp
 * @param updatedAt       Last updated timestamp
 */
public record PlotResponseDto(
        UUID id,
        UUID farmerId,
        UUID villageId,
        String villageName,
        String mandalName,
        String districtName,
        String stateName,
        String label,
        BigDecimal totalAreaAcres,
        boolean isActive,
        String version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

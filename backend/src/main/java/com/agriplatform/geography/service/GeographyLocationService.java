package com.agriplatform.geography.service;

import com.agriplatform.geography.dto.LocationResolutionDto;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.repository.VillageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Service for GPS-assisted administrative location resolution.
 *
 * <h2>Resolution Strategy</h2>
 * <ol>
 *   <li><strong>Evidence-Based Nearest Village Centroid:</strong> If active village records with
 *       valid latitude/longitude exist within a defensible distance threshold ({@value #MAX_VILLAGE_DISTANCE_KM} km),
 *       the nearest village and its validated parent hierarchy (Mandal → District → State) are returned.</li>
 *   <li><strong>Honest State-Level Fallback:</strong> If no village coordinate is within range
 *       (e.g. coordinates not yet mapped), the service resolves ONLY the candidate State.
 *       District, mandal, and village are strictly returned as {@code null} to prevent misleading
 *       assignments (e.g. assigning Adilabad across all of Telangana or Kottayam across Kerala).</li>
 *   <li><strong>Strict Advisory Semantics:</strong> All outputs enforce {@code manualConfirmationRequired = true}
 *       and {@code isExactBoundary = false}.</li>
 * </ol>
 */
@Service
public class GeographyLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeographyLocationService.class);

    /**
     * Maximum defensible distance threshold in kilometers between GPS coordinates and a village centroid.
     * Based on typical Indian revenue village diameter (2–5 km). Coordinates farther than 5 km
     * cannot be responsibly inferred as belonging to that specific village.
     */
    public static final double MAX_VILLAGE_DISTANCE_KM = 5.0;

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final VillageRepository villageRepository;

    public GeographyLocationService() {
        this.villageRepository = null;
    }

    @Autowired
    public GeographyLocationService(@Autowired(required = false) VillageRepository villageRepository) {
        this.villageRepository = villageRepository;
    }

    /**
     * Resolves GPS coordinates to the closest candidate administrative hierarchy.
     *
     * @param lat latitude
     * @param lon longitude
     * @return advisory candidate suggestion requiring farmer confirmation
     */
    public LocationResolutionDto resolveCandidateLocation(BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) {
            return LocationResolutionDto.createResolved(
                    null, null,
                    null, null,
                    null, null,
                    null, null,
                    null, null,
                    "UNRESOLVED",
                    "No coordinates provided. Please select your administrative location manually from the dropdowns."
            );
        }

        double dLat = lat.doubleValue();
        double dLon = lon.doubleValue();

        if (dLat < -90.0 || dLat > 90.0 || dLon < -180.0 || dLon > 180.0) {
            return LocationResolutionDto.createResolved(
                    lat, lon,
                    null, null,
                    null, null,
                    null, null,
                    null, null,
                    "UNRESOLVED",
                    "Coordinates are out of geographic bounds. Please select your administrative location manually."
            );
        }

        // 1. Evidence-Based Database Village Lookup (Haversine Distance with Bounding-Box Prefilter)
        if (villageRepository != null) {
            try {
                double latDelta = MAX_VILLAGE_DISTANCE_KM / 111.0;
                double lonDelta = MAX_VILLAGE_DISTANCE_KM / (111.0 * Math.max(0.1, Math.cos(Math.toRadians(dLat))));

                BigDecimal minLat = BigDecimal.valueOf(dLat - latDelta);
                BigDecimal maxLat = BigDecimal.valueOf(dLat + latDelta);
                BigDecimal minLon = BigDecimal.valueOf(dLon - lonDelta);
                BigDecimal maxLon = BigDecimal.valueOf(dLon + lonDelta);

                List<Village> nearby = villageRepository.findActiveWithinBoundingBox(minLat, maxLat, minLon, maxLon);
                if (nearby != null && !nearby.isEmpty()) {
                    Village bestVillage = null;
                    double bestDistance = Double.MAX_VALUE;

                    for (Village v : nearby) {
                        if (v.getLatitude() != null && v.getLongitude() != null) {
                            double dist = haversineDistanceKm(dLat, dLon, v.getLatitude().doubleValue(), v.getLongitude().doubleValue());
                            if (dist < bestDistance) {
                                bestDistance = dist;
                                bestVillage = v;
                            }
                        }
                    }

                    if (bestVillage != null && bestDistance <= MAX_VILLAGE_DISTANCE_KM) {
                        Mandal m = bestVillage.getMandal();
                        District d = m != null ? m.getDistrict() : null;
                        State s = d != null ? d.getState() : null;

                        if (m != null && d != null && s != null) {
                            return LocationResolutionDto.createResolved(
                                    lat, lon,
                                    s.getCode(), s.getName(),
                                    d.getCode(), d.getName(),
                                    m.getCode(), m.getName(),
                                    bestVillage.getCode(), bestVillage.getName(),
                                    "NEAREST_VILLAGE_CENTROID",
                                    String.format(Locale.ROOT,
                                            "Candidate village suggested (approx. %.2f km from coordinates). Please confirm or correct your administrative hierarchy.",
                                            bestDistance)
                            );
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Database coordinate search encountered an issue; falling back to state boundary detection: {}", e.getMessage());
            }
        }

        // 2. State-Level Geographic Bounds (Honest Uncertainty: District, Mandal & Village remain NULL)
        // Telangana bounds: Lat: 15.8 - 19.9, Lon: 77.2 - 81.8 (Evaluated before broader AP to prevent nested capture)
        if (dLat >= 15.8 && dLat <= 19.9 && dLon >= 77.2 && dLon <= 81.8) {
            return LocationResolutionDto.createResolved(
                    lat, lon,
                    "36", "Telangana",
                    null, null,
                    null, null,
                    null, null,
                    "STATE_APPROXIMATE",
                    "Location detected within Telangana. District and village coordinates are not yet mapped — please confirm your state and select your district manually."
            );
        }

        // Andhra Pradesh bounds: Lat: 12.6 - 19.9, Lon: 76.7 - 84.8
        if (dLat >= 12.6 && dLat <= 19.9 && dLon >= 76.7 && dLon <= 84.8) {
            return LocationResolutionDto.createResolved(
                    lat, lon,
                    "28", "Andhra Pradesh",
                    null, null,
                    null, null,
                    null, null,
                    "STATE_APPROXIMATE",
                    "Location detected within Andhra Pradesh. District and village coordinates are not yet mapped — please confirm your state and select your district manually."
            );
        }

        // Kerala bounds: Lat: 8.2 - 12.8, Lon: 74.8 - 77.5
        if (dLat >= 8.2 && dLat <= 12.8 && dLon >= 74.8 && dLon <= 77.5) {
            return LocationResolutionDto.createResolved(
                    lat, lon,
                    "32", "Kerala",
                    null, null,
                    null, null,
                    null, null,
                    "STATE_APPROXIMATE",
                    "Location detected within Kerala. District and village coordinates are not yet mapped — please confirm your state and select your district manually."
            );
        }

        // Tamil Nadu bounds: Lat: 8.0 - 13.6, Lon: 76.2 - 80.4
        if (dLat >= 8.0 && dLat <= 13.6 && dLon >= 76.2 && dLon <= 80.4) {
            return LocationResolutionDto.createResolved(
                    lat, lon,
                    "33", "Tamil Nadu",
                    null, null,
                    null, null,
                    null, null,
                    "STATE_APPROXIMATE",
                    "Location detected within Tamil Nadu. District and village coordinates are not yet mapped — please confirm your state and select your district manually."
            );
        }

        // Karnataka bounds: Lat: 11.5 - 18.5, Lon: 74.0 - 78.6
        if (dLat >= 11.5 && dLat <= 18.5 && dLon >= 74.0 && dLon <= 78.6) {
            return LocationResolutionDto.createResolved(
                    lat, lon,
                    "29", "Karnataka",
                    null, null,
                    null, null,
                    null, null,
                    "STATE_APPROXIMATE",
                    "Location detected within Karnataka. District and village coordinates are not yet mapped — please confirm your state and select your district manually."
            );
        }

        // Fallback for coordinates outside the pilot region
        return LocationResolutionDto.createResolved(
                lat, lon,
                null, null,
                null, null,
                null, null,
                null, null,
                "UNRESOLVED",
                "Coordinates fall outside the five-state pilot region. Please manually select your state and district from the dropdowns."
        );
    }

    /**
     * Computes the great-circle distance between two geographic coordinates using the Haversine formula.
     *
     * @param lat1 latitude of first point in decimal degrees
     * @param lon1 longitude of first point in decimal degrees
     * @param lat2 latitude of second point in decimal degrees
     * @param lon2 longitude of second point in decimal degrees
     * @return distance in kilometers
     */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }
}

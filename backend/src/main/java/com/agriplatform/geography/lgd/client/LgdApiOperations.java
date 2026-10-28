package com.agriplatform.geography.lgd.client;

import com.agriplatform.geography.lgd.dto.*;

import java.util.List;

/**
 * Contract for LGD API HTTP operations.
 *
 * <p>Decoupling via interface allows:
 * <ul>
 *   <li>Standard JDK proxy-based mocking in tests (no Byte Buddy required).</li>
 *   <li>Future alternative implementations (caching, circuit-breaker wrappers).</li>
 * </ul>
 */
public interface LgdApiOperations {

    /** Fetches all active states. POST /states/v1/status/active */
    List<LgdStateDto> fetchActiveStates();

    /** Fetches active districts by state. POST /state/{stateCode}/districts/v1/status/active */
    List<LgdDistrictDto> fetchActiveDistrictsByState(int stateCode);

    /** Fetches active sub-districts by state. POST /state/{stateCode}/subdistricts/v1/status/active */
    List<LgdSubDistrictDto> fetchActiveSubDistrictsByState(int stateCode);

    /** Fetches active villages by state (v2). POST /state/{stateCode}/villages/v2/status/active */
    List<LgdVillageDto> fetchActiveVillagesByState(int stateCode);

    /** Fetches one hierarchy page for a state. POST /stateDistrictSubdistBlockVillage/{stateCode}/page/{page} */
    List<LgdHierarchyRowDto> fetchHierarchyPage(int stateCode, int page);

    /** Fetches states updated in date range. POST /states/v1/updatedFrom/{from}/updatedTo/{to} */
    List<LgdStateDto> fetchStatesByDuration(String from, String to);

    /** Fetches districts updated in date range for a state. */
    List<LgdDistrictDto> fetchDistrictsByStateDuration(int stateCode, String from, String to);

    /** Fetches sub-districts updated in date range for a state (v2). */
    List<LgdSubDistrictDto> fetchSubDistrictsByStateDuration(int stateCode, String from, String to);

    /** Fetches villages updated in date range for a state (v2). */
    List<LgdVillageDto> fetchVillagesByStateDuration(int stateCode, String from, String to);
}

package org.systemsphd.explorer.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Venue-year coverage read from the generated {@code venue-years.json}
 * manifest. Lets the interface list venue-years that have no papers yet
 * instead of silently omitting them.
 */
public final class VenueCoverage {
    private final Map<String, Map<Integer, VenueYearCoverage>> byVenue;

    private VenueCoverage(Map<String, Map<Integer, VenueYearCoverage>> byVenue) {
        this.byVenue = byVenue;
    }

    public static VenueCoverage empty() {
        return new VenueCoverage(Map.of());
    }

    public static VenueCoverage load(InputStream stream) throws IOException {
        JsonNode root = new ObjectMapper().readTree(stream);
        Map<String, Map<Integer, VenueYearCoverage>> byVenue = new HashMap<>();
        for (JsonNode entry : root.path("venue_years")) {
            String venue = entry.path("venue").asText("").trim();
            int year = entry.path("year").asInt(0);
            String status = entry.path("collection_status").asText("").trim();
            if (venue.isEmpty() || year == 0 || status.isEmpty()) {
                continue;
            }
            byVenue.computeIfAbsent(venue, key -> new TreeMap<>(Collections.reverseOrder()))
                    .put(year, new VenueYearCoverage(
                            venue, year, status, entry.path("last_verified_at").asText("").trim()
                    ));
        }
        return new VenueCoverage(byVenue);
    }

    public int size() {
        return byVenue.values().stream().mapToInt(Map::size).sum();
    }

    /** Coverage for one venue-year, or null when the manifest does not list it. */
    public VenueYearCoverage find(String venue, int year) {
        Map<Integer, VenueYearCoverage> years = byVenue.get(venue);
        return years == null ? null : years.get(year);
    }

    public Set<Integer> yearsFor(String venue) {
        Map<Integer, VenueYearCoverage> years = byVenue.get(venue);
        return years == null ? Set.of() : Collections.unmodifiableSet(years.keySet());
    }
}

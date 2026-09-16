package org.systemsphd.explorer.support;

import org.systemsphd.explorer.catalog.VenueCoverage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** A small venue-year manifest covering every collection status. */
public final class CoverageFixture {
    public static final String MANIFEST = """
            {"schema_version": "1.0.0", "venue_years": [
              {"venue": "osdi", "year": 2025, "collection_status": "COMPLETE_PROCEEDINGS", "last_verified_at": "2026-07-29T23:38:46Z"},
              {"venue": "osdi", "year": 2026, "collection_status": "COMPLETE_PROCEEDINGS", "last_verified_at": "2026-07-29T23:38:46Z"},
              {"venue": "ccs", "year": 2026, "collection_status": "UPCOMING", "last_verified_at": "2026-07-30T03:19:32Z"},
              {"venue": "atc", "year": 2026, "collection_status": "UNAVAILABLE", "last_verified_at": "2026-07-30T03:18:26Z"},
              {"venue": "sosp", "year": 2026, "collection_status": "ACCEPTED_PAPERS_ONLY", "last_verified_at": "2026-07-30T03:18:26Z"},
              {"venue": "sigcomm", "year": 2026, "collection_status": "PARTIAL", "last_verified_at": ""},
              {"venue": "", "year": 2026, "collection_status": "PARTIAL", "last_verified_at": ""},
              {"venue": "nsdi", "year": 2026, "collection_status": "", "last_verified_at": ""}
            ]}
            """;

    private CoverageFixture() {
    }

    public static VenueCoverage load() {
        try {
            return VenueCoverage.load(new ByteArrayInputStream(MANIFEST.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

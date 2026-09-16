package org.systemsphd.explorer.catalog;

import org.junit.jupiter.api.Test;
import org.systemsphd.explorer.support.CoverageFixture;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VenueCoverageTest {
    @Test
    void loadsEveryWellFormedVenueYear() {
        VenueCoverage coverage = CoverageFixture.load();

        assertEquals(6, coverage.size());
        assertEquals(List.of(2026, 2025), List.copyOf(coverage.yearsFor("osdi")));
        assertEquals(Set.of(), coverage.yearsFor("fast"));
        assertNull(coverage.find("nsdi", 2026));
        assertNull(coverage.find("osdi", 2024));
    }

    @Test
    void labelsDistinguishEveryStatus() {
        VenueCoverage coverage = CoverageFixture.load();

        VenueYearCoverage osdi = coverage.find("osdi", 2025);
        assertTrue(osdi.isComplete());
        assertFalse(osdi.isNoProgram());
        assertEquals("Complete proceedings", osdi.getLabel());
        assertEquals("", osdi.getMark());
        assertEquals("2026-07-29", osdi.getLastVerifiedDate());

        assertEquals("Program not published yet", coverage.find("ccs", 2026).getLabel());
        assertEquals("upcoming", coverage.find("ccs", 2026).getMark());
        assertTrue(coverage.find("ccs", 2026).isNoProgram());

        assertEquals("Program unavailable", coverage.find("atc", 2026).getLabel());
        assertTrue(coverage.find("atc", 2026).isNoProgram());

        assertEquals("Accepted papers only", coverage.find("sosp", 2026).getLabel());
        assertEquals("accepted only", coverage.find("sosp", 2026).getMark());
        assertFalse(coverage.find("sosp", 2026).isNoProgram());

        assertEquals("Partial coverage", coverage.find("sigcomm", 2026).getLabel());
        assertEquals("", coverage.find("sigcomm", 2026).getLastVerifiedDate());
    }

    @Test
    void emptyCoverageKnowsNothing() {
        assertEquals(0, VenueCoverage.empty().size());
        assertNull(VenueCoverage.empty().find("osdi", 2025));
        assertTrue(VenueCoverage.empty().yearsFor("osdi").isEmpty());
    }
}

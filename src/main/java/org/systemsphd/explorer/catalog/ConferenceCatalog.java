package org.systemsphd.explorer.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConferenceCatalog {
    private final List<ConferenceCategory> categories;
    private final Map<String, ConferenceDefinition> conferencesBySlug;
    private final Map<String, ConferenceCategory> categoriesBySlug;

    public ConferenceCatalog() {
        categories = List.of(
                category("operating-systems", "Operating systems",
                        conference("osdi", "OSDI", "USENIX Symposium on Operating Systems Design and Implementation", "operating-systems"),
                        conference("sosp", "SOSP", "ACM Symposium on Operating Systems Principles", "operating-systems"),
                        conference("eurosys", "EuroSys", "ACM European Conference on Computer Systems", "operating-systems"),
                        conference("atc", "USENIX ATC", "USENIX Annual Technical Conference", "operating-systems"),
                        conference("fast", "FAST", "USENIX Conference on File and Storage Technologies", "operating-systems")),
                category("computer-networks", "Computer networks",
                        conference("sigcomm", "SIGCOMM", "ACM SIGCOMM", "computer-networks"),
                        conference("nsdi", "NSDI", "USENIX Symposium on Networked Systems Design and Implementation", "computer-networks")),
                category("computer-security", "Computer security",
                        conference("ccs", "ACM CCS", "ACM Conference on Computer and Communications Security", "computer-security"),
                        conference("ndss", "NDSS", "Network and Distributed System Security Symposium", "computer-security"),
                        conference("sp", "IEEE S&P", "IEEE Symposium on Security and Privacy", "computer-security"),
                        conference("usenix-security", "USENIX Security", "USENIX Security Symposium", "computer-security")),
                category("computer-architecture", "Computer architecture",
                        conference("asplos", "ASPLOS", "Architectural Support for Programming Languages and Operating Systems", "computer-architecture"))
        );
        conferencesBySlug = new LinkedHashMap<>();
        categoriesBySlug = new LinkedHashMap<>();
        for (ConferenceCategory category : categories) {
            categoriesBySlug.put(category.getSlug(), category);
            for (ConferenceDefinition conference : category.getConferences()) {
                conferencesBySlug.put(conference.getSlug(), conference);
            }
        }
    }

    private static ConferenceCategory category(
            String slug,
            String name,
            ConferenceDefinition... conferences
    ) {
        return new ConferenceCategory(slug, name, List.of(conferences));
    }

    private static ConferenceDefinition conference(
            String slug,
            String name,
            String fullName,
            String categorySlug
    ) {
        return new ConferenceDefinition(slug, name, fullName, categorySlug);
    }

    public List<ConferenceCategory> getCategories() {
        return categories;
    }

    public List<ConferenceDefinition> getConferences() {
        return conferencesBySlug.values().stream().toList();
    }

    public boolean hasCategory(String slug) {
        return categoriesBySlug.containsKey(slug);
    }

    public boolean hasConference(String slug) {
        return conferencesBySlug.containsKey(slug);
    }

    public String categoryForVenue(String venue) {
        ConferenceDefinition conference = conferencesBySlug.get(venue);
        return conference == null ? "" : conference.getCategorySlug();
    }

    public String categoryName(String category) {
        ConferenceCategory found = categoriesBySlug.get(category);
        return found == null ? "" : found.getName();
    }

    public String venueFullName(String venue) {
        ConferenceDefinition conference = conferencesBySlug.get(venue);
        return conference == null ? "" : conference.getFullName();
    }

    public String venueName(String venue) {
        ConferenceDefinition conference = conferencesBySlug.get(venue);
        return conference == null ? venue.toUpperCase(Locale.ROOT) : conference.getName();
    }
}

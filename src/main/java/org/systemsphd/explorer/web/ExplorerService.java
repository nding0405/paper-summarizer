package org.systemsphd.explorer.web;

import org.systemsphd.explorer.catalog.ConferenceCatalog;
import org.systemsphd.explorer.catalog.ConferenceCategory;
import org.systemsphd.explorer.catalog.ConferenceDefinition;
import org.systemsphd.explorer.catalog.Taxonomy;
import org.systemsphd.explorer.catalog.VenueCoverage;
import org.systemsphd.explorer.catalog.VenueYearCoverage;
import org.systemsphd.explorer.model.Paper;
import org.systemsphd.explorer.model.RepositorySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Search, filtering, sorting, pagination, and sidebar navigation over one
 * loaded paper snapshot. Instances are immutable and safe to share between
 * requests.
 */
public final class ExplorerService {
    static final int PAGE_SIZE = 40;
    static final int[] JUMP_SIZES = {5, 10};
    static final int SUGGESTION_LIMIT = 10;
    private static final Pattern QUERY_TOKEN = Pattern.compile("\"([^\"]+)\"|(\\S+)");
    private static final Pattern NUMBER_RUN = Pattern.compile("\\d+|\\D+");
    /** Ranking weights: a term found in the title outranks one found in the abstract, which outranks the rest. */
    private static final int TITLE_WEIGHT = 4;
    private static final int ABSTRACT_WEIGHT = 2;
    private static final int OTHER_WEIGHT = 1;

    private final RepositorySnapshot snapshot;
    private final ConferenceCatalog catalog;
    private final VenueCoverage coverage;
    private final FacetIndex facets;
    private final CorpusStatistics statistics;
    private final Map<String, Paper> papersById;
    private final List<Integer> years;
    /** Default-visible paper counts keyed by venue slug, then year. */
    private final Map<String, Map<Integer, Long>> navigationCounts;

    public ExplorerService(RepositorySnapshot snapshot, ConferenceCatalog catalog) {
        this(snapshot, catalog, VenueCoverage.empty(), Taxonomy.empty());
    }

    public ExplorerService(
            RepositorySnapshot snapshot,
            ConferenceCatalog catalog,
            VenueCoverage coverage,
            Taxonomy taxonomy
    ) {
        this.snapshot = snapshot;
        this.catalog = catalog;
        this.coverage = coverage;
        this.facets = FacetIndex.build(
                snapshot.getPapers().stream().filter(this::isVisibleByDefault).toList(), taxonomy
        );
        this.statistics = new CorpusStatistics(snapshot.getPapers());
        Map<String, Paper> byId = new HashMap<>();
        snapshot.getPapers().forEach(paper -> byId.putIfAbsent(paper.getId(), paper));
        this.papersById = Map.copyOf(byId);
        this.years = snapshot.getPapers().stream()
                .map(Paper::getYear)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        this.navigationCounts = new HashMap<>();
        for (Paper paper : snapshot.getPapers()) {
            if (isVisibleByDefault(paper)) {
                navigationCounts
                        .computeIfAbsent(paper.getVenue(), venue -> new HashMap<>())
                        .merge(paper.getYear(), 1L, Long::sum);
            }
        }
    }

    /** Keyword facets with counts, for the filter panel and parameter validation. */
    public FacetIndex getFacets() {
        return facets;
    }

    public ConferenceCatalog getCatalog() {
        return catalog;
    }

    /** Corpus-wide counts for the page footer. */
    public CorpusStatistics getStatistics() {
        return statistics;
    }

    /** The paper with this export ID, or null. */
    public Paper find(String id) {
        return id == null ? null : papersById.get(id.trim());
    }

    public Map<String, String> venueNames() {
        Map<String, String> names = new LinkedHashMap<>();
        catalog.getConferences().forEach(conference -> names.put(conference.getSlug(), conference.getName()));
        return names;
    }

    public ExplorerPage page(FilterState filters) {
        List<String> tokens = tokens(filters.getQuery());
        Comparator<Paper> byRecency = Comparator.comparingInt(Paper::getYear).reversed()
                .thenComparing(Paper::getVenue)
                .thenComparing(Paper::getTitle, String.CASE_INSENSITIVE_ORDER);
        // With a query, the best textual match comes first; otherwise newest first.
        Comparator<Paper> order = tokens.isEmpty()
                ? byRecency
                : Comparator.comparingInt((Paper paper) -> score(paper, tokens)).reversed().thenComparing(byRecency);
        List<Paper> matches = snapshot.getPapers().stream()
                .filter(paper -> matches(paper, filters, tokens))
                .sorted(order)
                .toList();

        int totalResults = matches.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalResults / (double) PAGE_SIZE));
        int currentPage = Math.min(filters.getPage(), totalPages);
        int fromIndex = Math.min((currentPage - 1) * PAGE_SIZE, totalResults);
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalResults);
        List<Paper> visible = matches.subList(fromIndex, toIndex);
        int firstResult = totalResults == 0 ? 0 : fromIndex + 1;
        String previousUrl = currentPage > 1 ? filters.pageUrl(currentPage - 1) : null;
        String nextUrl = currentPage < totalPages ? filters.pageUrl(currentPage + 1) : null;
        List<PageJump> backwardJumps = new ArrayList<>();
        List<PageJump> forwardJumps = new ArrayList<>();
        for (int size : JUMP_SIZES) {
            if (currentPage - size >= 1) {
                backwardJumps.add(0, new PageJump("\u2212" + size, currentPage - size, filters.pageUrl(currentPage - size)));
            }
            if (currentPage + size <= totalPages) {
                forwardJumps.add(new PageJump("+" + size, currentPage + size, filters.pageUrl(currentPage + size)));
            }
        }

        VenueYearCoverage selectedCoverage = filters.getVenue().isBlank() || filters.getYear() == null
                ? null
                : coverage.find(filters.getVenue(), filters.getYear());

        return new ExplorerPage(
                heading(filters),
                selectedCoverage,
                visible,
                navigation(filters),
                activeFilters(filters),
                catalog.getCategories(),
                catalog.getConferences(),
                years,
                filters,
                totalResults,
                firstResult,
                toIndex,
                totalPages,
                currentPage,
                previousUrl,
                nextUrl,
                backwardJumps,
                forwardJumps,
                snapshot.isFixture()
        );
    }

    boolean matches(Paper paper, FilterState filters) {
        return matches(paper, filters, tokens(filters.getQuery()));
    }

    private boolean matches(Paper paper, FilterState filters, List<String> tokens) {
        if (!filters.getVenue().isBlank() && !paper.getVenue().equals(filters.getVenue())) {
            return false;
        }
        if (filters.getVenue().isBlank()
                && !filters.getCategory().isBlank()
                && !catalog.categoryForVenue(paper.getVenue()).equals(filters.getCategory())) {
            return false;
        }
        if (filters.getYear() != null && paper.getYear() != filters.getYear()) {
            return false;
        }
        if (filters.getRelevance().isBlank()) {
            if ("EXCLUDED".equals(paper.getRelevanceClass())) {
                return false;
            }
        } else if (!"ALL".equals(filters.getRelevance())
                && !paper.getRelevanceClass().equals(filters.getRelevance())) {
            return false;
        }
        if (!matchesAvailability(paper, filters.getAvailability())) {
            return false;
        }
        if (!matchesKeywords(paper.getTopics(), filters.getTopics(), filters.isMatchAll())
                || !matchesKeywords(paper.getSystemLayers(), filters.getLayers(), filters.isMatchAll())
                || !matchesKeywords(paper.getMethods(), filters.getMethods(), filters.isMatchAll())) {
            return false;
        }
        return matchesTokens(paper, tokens);
    }

    /** Facets combine with AND; within one facet the selected keywords match any or all. */
    private static boolean matchesKeywords(List<String> values, List<String> selected, boolean matchAll) {
        if (selected.isEmpty()) {
            return true;
        }
        return matchAll ? values.containsAll(selected) : selected.stream().anyMatch(values::contains);
    }

    private boolean matchesAvailability(Paper paper, String availability) {
        return switch (availability) {
            case "code" -> paper.isHasCode();
            case "artifact" -> paper.isHasArtifact();
            case "either" -> paper.isHasCode() || paper.isHasArtifact();
            case "summary" -> paper.isHasSummary();
            case "abstract" -> paper.isHasAbstract();
            default -> true;
        };
    }

    /**
     * Splits a query into lower-case terms. A quoted phrase is one term; stray
     * quote characters are ignored, so a query of only quotes yields no terms.
     */
    static List<String> tokens(String query) {
        List<String> tokens = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return tokens;
        }
        Matcher matcher = QUERY_TOKEN.matcher(query.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group(1) != null
                    ? matcher.group(1).trim()
                    : matcher.group(2).replace("\"", "");
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /** Every term must occur somewhere in the paper's searchable text. */
    private static boolean matchesTokens(Paper paper, List<String> tokens) {
        String haystack = paper.getSearchText();
        for (String token : tokens) {
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    /** Ranking of a matching paper: title hits count most, then abstract hits, then anything else. */
    static int score(Paper paper, List<String> tokens) {
        int score = 0;
        for (String token : tokens) {
            if (paper.getTitleSearchText().contains(token)) {
                score += TITLE_WEIGHT;
            } else if (paper.getAbstractSearchText().contains(token)) {
                score += ABSTRACT_WEIGHT;
            } else {
                score += OTHER_WEIGHT;
            }
        }
        return score;
    }

    /**
     * Best default-visible matches for a partial query, for the search box's
     * live suggestion list.
     */
    public List<Paper> suggest(String query) {
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }
        Comparator<Paper> order = Comparator.comparingInt((Paper paper) -> score(paper, tokens)).reversed()
                .thenComparing(Comparator.comparingInt(Paper::getYear).reversed())
                .thenComparing(Paper::getTitle, String.CASE_INSENSITIVE_ORDER);
        return snapshot.getPapers().stream()
                .filter(this::isVisibleByDefault)
                .filter(paper -> matchesTokens(paper, tokens))
                .sorted(order)
                .limit(SUGGESTION_LIMIT)
                .toList();
    }

    /**
     * One conference-year laid out like its program: every paper, including
     * excluded ones, grouped by track or session in natural order.
     */
    public ProceedingsPage proceedings(String venueParameter, String yearParameter, String basePath, String searchBasePath) {
        String venue = venueParameter == null ? "" : venueParameter.trim();
        if (!catalog.hasConference(venue)) {
            venue = "";
        }
        FilterState empty = FilterState.from(name -> null, catalog, facets, basePath);
        List<NavigationCategory> overview = navigation(empty);
        if (venue.isBlank()) {
            return new ProceedingsPage("", "", "", null, null, List.of(), overview, List.of(),
                    catalog.getCategories(), venueNames(), searchBasePath);
        }

        String selectedVenue = venue;
        List<NavigationYear> venueYears = overview.stream()
                .flatMap(category -> category.getVenues().stream())
                .filter(entry -> entry.getSlug().equals(selectedVenue))
                .findFirst()
                .map(NavigationVenue::getYears)
                .orElse(List.of());
        Integer requestedYear = parseYear(yearParameter);
        boolean known = requestedYear != null
                && venueYears.stream().anyMatch(entry -> entry.getYear() == requestedYear);
        Integer year = known ? requestedYear : venueYears.stream()
                .filter(entry -> entry.getCount() > 0)
                .map(NavigationYear::getYear)
                .findFirst()
                .orElse(venueYears.isEmpty() ? null : venueYears.get(0).getYear());
        Integer selectedYear = year;
        List<NavigationYear> switcher = venueYears.stream()
                .map(entry -> new NavigationYear(
                        entry.getYear(), entry.getCount(),
                        basePath + "?venue=" + selectedVenue + "&year=" + entry.getYear(),
                        selectedYear != null && entry.getYear() == selectedYear, entry.getCoverage()))
                .toList();

        Map<String, List<Paper>> grouped = new TreeMap<>(ExplorerService::compareNaturally);
        if (year != null) {
            for (Paper paper : snapshot.getPapers()) {
                if (paper.getVenue().equals(venue) && paper.getYear() == year) {
                    grouped.computeIfAbsent(paper.getTrack(), track -> new ArrayList<>()).add(paper);
                }
            }
        }
        List<ProceedingsSession> sessions = new ArrayList<>();
        Map<String, Integer> anchors = new HashMap<>();
        for (Map.Entry<String, List<Paper>> entry : grouped.entrySet()) {
            List<Paper> papers = entry.getValue().stream()
                    .sorted(Comparator.comparing(Paper::getTitle, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            String base = entry.getKey().isBlank() ? "papers" : "session-" + slug(entry.getKey());
            int seen = anchors.merge(base, 1, Integer::sum);
            sessions.add(new ProceedingsSession(entry.getKey(), seen == 1 ? base : base + "-" + seen, papers));
        }
        String searchUrl = searchBasePath + "?category=" + catalog.categoryForVenue(venue) + "&venue=" + venue
                + (year == null ? "" : "&year=" + year) + "&relevance=ALL";
        return new ProceedingsPage(
                venue, catalog.venueName(venue), catalog.venueFullName(venue), year,
                year == null ? null : coverage.find(venue, year),
                sessions, overview, switcher, catalog.getCategories(), venueNames(), searchUrl
        );
    }

    private static Integer parseYear(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** "Session 2D" sorts before "Session 10A"; blank names (no session data) sort last. */
    static int compareNaturally(String left, String right) {
        if (left.isBlank() != right.isBlank()) {
            return left.isBlank() ? 1 : -1;
        }
        Matcher leftRuns = NUMBER_RUN.matcher(left.toLowerCase(Locale.ROOT));
        Matcher rightRuns = NUMBER_RUN.matcher(right.toLowerCase(Locale.ROOT));
        while (leftRuns.find() && rightRuns.find()) {
            String a = leftRuns.group();
            String b = rightRuns.group();
            int comparison;
            if (Character.isDigit(a.charAt(0)) && Character.isDigit(b.charAt(0))) {
                comparison = a.length() == b.length() ? a.compareTo(b) : Integer.compare(a.length(), b.length());
            } else {
                comparison = a.compareTo(b);
            }
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(left.length(), right.length());
    }

    private static String slug(String value) {
        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return slug.isEmpty() ? "session" : slug;
    }

    private String heading(FilterState filters) {
        StringBuilder heading = new StringBuilder();
        if (!filters.getVenue().isBlank()) {
            heading.append(catalog.venueName(filters.getVenue()));
        } else if (!filters.getCategory().isBlank()) {
            heading.append(catalog.categoryName(filters.getCategory()));
        }
        if (filters.getYear() != null) {
            if (heading.length() > 0) {
                heading.append(' ');
            }
            heading.append(filters.getYear());
        }
        return heading.length() == 0 ? "All papers" : heading.toString();
    }

    private List<ActiveFilter> activeFilters(FilterState filters) {
        List<ActiveFilter> active = new ArrayList<>();
        if (!filters.getQuery().isBlank()) {
            active.add(new ActiveFilter("Search", filters.getQuery(), filters.urlWithout("q")));
        }
        if (!filters.getVenue().isBlank()) {
            active.add(new ActiveFilter(
                    "Conference", catalog.venueName(filters.getVenue()), filters.urlWithout("venue")
            ));
        } else if (!filters.getCategory().isBlank()) {
            active.add(new ActiveFilter(
                    "Category", catalog.categoryName(filters.getCategory()), filters.urlWithout("category")
            ));
        }
        if (filters.getYear() != null) {
            active.add(new ActiveFilter("Year", filters.getYearValue(), filters.urlWithout("year")));
        }
        if (!filters.getRelevance().isBlank()) {
            active.add(new ActiveFilter(
                    "Relevance", relevanceLabel(filters.getRelevance()), filters.urlWithout("relevance")
            ));
        }
        if (!filters.getAvailability().isBlank()) {
            active.add(new ActiveFilter(
                    "Availability", availabilityLabel(filters.getAvailability()), filters.urlWithout("availability")
            ));
        }
        for (FacetIndex.Facet facet : facets.getFacets()) {
            String name = switch (facet.getKey()) {
                case FacetIndex.TOPIC -> "Topic";
                case FacetIndex.LAYER -> "Layer";
                case FacetIndex.METHOD -> "Method";
                default -> facet.getName();
            };
            for (String id : filters.selected(facet.getKey())) {
                active.add(new ActiveFilter(name, facet.labelFor(id), filters.urlWithout(facet.getKey(), id)));
            }
        }
        if (filters.isMatchAll() && filters.getKeywordCount() > 1) {
            active.add(new ActiveFilter("Keywords", "match all", filters.urlWithout("match")));
        }
        return active;
    }

    static String relevanceLabel(String relevance) {
        return "ALL".equals(relevance) ? "All classes, including excluded" : Paper.relevanceLabel(relevance);
    }

    static String availabilityLabel(String availability) {
        return switch (availability) {
            case "code" -> "Has code";
            case "artifact" -> "Has artifact";
            case "either" -> "Has code or artifact";
            case "summary" -> "Has LLM summary";
            case "abstract" -> "Has abstract";
            default -> "";
        };
    }

    private List<NavigationCategory> navigation(FilterState filters) {
        List<NavigationCategory> navigation = new ArrayList<>();
        for (ConferenceCategory category : catalog.getCategories()) {
            List<NavigationVenue> venues = new ArrayList<>();
            long categoryCount = 0;
            for (ConferenceDefinition conference : category.getConferences()) {
                Map<Integer, Long> countsByYear = navigationCounts.getOrDefault(conference.getSlug(), Map.of());
                // Years with papers plus years the coverage manifest knows about, newest first.
                TreeSet<Integer> venueYearSet = new TreeSet<>(Comparator.reverseOrder());
                venueYearSet.addAll(countsByYear.keySet());
                venueYearSet.addAll(coverage.yearsFor(conference.getSlug()));
                List<NavigationYear> venueYears = new ArrayList<>();
                long venueCount = 0;
                for (Integer year : venueYearSet) {
                    long count = countsByYear.getOrDefault(year, 0L);
                    venueCount += count;
                    venueYears.add(new NavigationYear(
                            year,
                            count,
                            filters.urlFor(category.getSlug(), conference.getSlug(), year),
                            conference.getSlug().equals(filters.getVenue())
                                    && year.equals(filters.getYear()),
                            coverage.find(conference.getSlug(), year)
                    ));
                }
                categoryCount += venueCount;
                venues.add(new NavigationVenue(
                        conference.getSlug(),
                        conference.getName(),
                        conference.getFullName(),
                        venueCount,
                        filters.urlFor(category.getSlug(), conference.getSlug(), null),
                        conference.getSlug().equals(filters.getVenue()) && filters.getYear() == null,
                        venueYears
                ));
            }
            navigation.add(new NavigationCategory(
                    category.getSlug(),
                    category.getName(),
                    categoryCount,
                    filters.urlFor(category.getSlug(), "", null),
                    category.getSlug().equals(filters.getCategory()) && filters.getVenue().isBlank(),
                    venues
            ));
        }
        return navigation;
    }

    private boolean isVisibleByDefault(Paper paper) {
        return !"EXCLUDED".equals(paper.getRelevanceClass())
                && catalog.hasConference(paper.getVenue());
    }
}

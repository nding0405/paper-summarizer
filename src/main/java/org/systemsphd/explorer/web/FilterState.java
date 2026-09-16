package org.systemsphd.explorer.web;

import org.systemsphd.explorer.catalog.ConferenceCatalog;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Validated GET parameters for one request plus the shareable URLs derived
 * from them. Unknown or malformed values fall back to "no filter" rather than
 * producing an error page. Keyword facets accept repeated parameters, e.g.
 * {@code topic=ml-systems&topic=gpu-scheduling}.
 */
public final class FilterState {
    static final int MAX_QUERY_LENGTH = 200;
    static final int MAX_FACET_VALUES = 25;
    static final Set<String> RELEVANCE_VALUES = Set.of(
            "ALL", "CORE_SYSTEMS", "SYSTEMS_ADJACENT", "NEEDS_HUMAN_REVIEW", "EXCLUDED"
    );
    static final Set<String> AVAILABILITY_VALUES = Set.of("code", "artifact", "either", "summary", "abstract");
    /** Keyword matching within a facet: any selected keyword (default) or all of them. */
    static final String MATCH_ANY = "any";
    static final String MATCH_ALL = "all";

    private final String basePath;
    private final String query;
    private final String category;
    private final String venue;
    private final Integer year;
    private final String relevance;
    private final String availability;
    private final List<String> topics;
    private final List<String> layers;
    private final List<String> methods;
    private final String match;
    private final int page;

    private FilterState(
            String basePath,
            String query,
            String category,
            String venue,
            Integer year,
            String relevance,
            String availability,
            List<String> topics,
            List<String> layers,
            List<String> methods,
            String match,
            int page
    ) {
        this.basePath = basePath;
        this.query = query;
        this.category = category;
        this.venue = venue;
        this.year = year;
        this.relevance = relevance;
        this.availability = availability;
        this.topics = List.copyOf(topics);
        this.layers = List.copyOf(layers);
        this.methods = List.copyOf(methods);
        this.match = match;
        this.page = page;
    }

    /**
     * @param parameters request parameter lookup returning every value of a
     *                   name, typically {@code request::getParameterValues}
     * @param catalog    known categories and conferences
     * @param facets     known keyword facets; unknown keywords are dropped
     * @param basePath   absolute path of the explorer page, e.g.
     *                   {@code "/index.jsp"}; every generated URL starts with it
     */
    public static FilterState from(
            Function<String, String[]> parameters,
            ConferenceCatalog catalog,
            FacetIndex facets,
            String basePath
    ) {
        Function<String, String> parameter = name -> {
            String[] values = parameters.apply(name);
            return values == null || values.length == 0 ? null : values[0];
        };

        String query = clean(parameter.apply("q"));
        if (query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH);
        }

        String category = clean(parameter.apply("category"));
        if (!catalog.hasCategory(category)) {
            category = "";
        }

        String venue = clean(parameter.apply("venue"));
        if (!catalog.hasConference(venue)) {
            venue = "";
        }
        if (!venue.isBlank()) {
            category = catalog.categoryForVenue(venue);
        }

        Integer year = parseYear(parameter.apply("year"));
        String relevance = clean(parameter.apply("relevance"));
        if (!RELEVANCE_VALUES.contains(relevance)) {
            relevance = "";
        }
        String availability = clean(parameter.apply("availability"));
        if (!AVAILABILITY_VALUES.contains(availability)) {
            availability = "";
        }
        List<String> topics = facetValues(parameters.apply(FacetIndex.TOPIC), facets, FacetIndex.TOPIC);
        List<String> layers = facetValues(parameters.apply(FacetIndex.LAYER), facets, FacetIndex.LAYER);
        List<String> methods = facetValues(parameters.apply(FacetIndex.METHOD), facets, FacetIndex.METHOD);
        String match = MATCH_ALL.equals(clean(parameter.apply("match"))) ? MATCH_ALL : MATCH_ANY;
        int page = parsePositiveInt(parameter.apply("page"), 1);
        return new FilterState(
                basePath, query, category, venue, year, relevance, availability,
                topics, layers, methods, match, page
        );
    }

    private static List<String> facetValues(String[] values, FacetIndex facets, String key) {
        if (values == null) {
            return List.of();
        }
        Set<String> accepted = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isEmpty() && facets.isValid(key, cleaned)) {
                accepted.add(cleaned);
            }
            if (accepted.size() >= MAX_FACET_VALUES) {
                break;
            }
        }
        return new ArrayList<>(accepted);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static Integer parseYear(String value) {
        try {
            int parsed = Integer.parseInt(clean(value));
            return parsed >= 2000 && parsed <= 2100 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(clean(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /** Sidebar link: keeps everything except the conference scope, and resets paging. */
    public String urlFor(String selectedCategory, String selectedVenue, Integer selectedYear) {
        return buildUrl(pairs(selectedCategory, selectedVenue, selectedYear, 1));
    }

    /** Pagination link: keeps every filter. */
    public String pageUrl(int selectedPage) {
        return buildUrl(pairs(category, venue, year, selectedPage));
    }

    /** Removes every value of one parameter, keeping the others and returning to page 1. */
    public String urlWithout(String parameterName) {
        List<Map.Entry<String, String>> pairs = pairs(category, venue, year, 1);
        pairs.removeIf(pair -> pair.getKey().equals(parameterName));
        return buildUrl(pairs);
    }

    /** Removes one value of a repeated parameter, e.g. a single selected topic. */
    public String urlWithout(String parameterName, String value) {
        List<Map.Entry<String, String>> pairs = pairs(category, venue, year, 1);
        pairs.removeIf(pair -> pair.getKey().equals(parameterName) && pair.getValue().equals(value));
        return buildUrl(pairs);
    }

    /** The unfiltered explorer page. */
    public String clearUrl() {
        return basePath;
    }

    /**
     * Every active parameter except the page, in URL order; the view renders
     * these as hidden inputs so a form submit keeps the current filters.
     */
    public List<Map.Entry<String, String>> getHiddenParameters() {
        return pairs(category, venue, year, 1);
    }

    private List<Map.Entry<String, String>> pairs(
            String category,
            String venue,
            Integer year,
            int page
    ) {
        List<Map.Entry<String, String>> pairs = new ArrayList<>();
        put(pairs, "q", query);
        put(pairs, "category", category);
        put(pairs, "venue", venue);
        if (year != null) {
            pairs.add(pair("year", String.valueOf(year)));
        }
        put(pairs, "relevance", relevance);
        put(pairs, "availability", availability);
        topics.forEach(topic -> pairs.add(pair(FacetIndex.TOPIC, topic)));
        layers.forEach(layer -> pairs.add(pair(FacetIndex.LAYER, layer)));
        methods.forEach(method -> pairs.add(pair(FacetIndex.METHOD, method)));
        if (MATCH_ALL.equals(match)) {
            pairs.add(pair("match", match));
        }
        if (page > 1) {
            pairs.add(pair("page", String.valueOf(page)));
        }
        return pairs;
    }

    private String buildUrl(List<Map.Entry<String, String>> pairs) {
        if (pairs.isEmpty()) {
            return basePath;
        }
        StringBuilder url = new StringBuilder(basePath).append('?');
        boolean first = true;
        for (Map.Entry<String, String> pair : pairs) {
            if (!first) {
                url.append('&');
            }
            first = false;
            url.append(encode(pair.getKey())).append('=').append(encode(pair.getValue()));
        }
        return url.toString();
    }

    private static void put(List<Map.Entry<String, String>> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.add(pair(key, value));
        }
    }

    private static Map.Entry<String, String> pair(String key, String value) {
        return new SimpleImmutableEntry<>(key, value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public String getBasePath() {
        return basePath;
    }

    public String getQuery() {
        return query;
    }

    public String getCategory() {
        return category;
    }

    public String getVenue() {
        return venue;
    }

    public Integer getYear() {
        return year;
    }

    public String getYearValue() {
        return year == null ? "" : String.valueOf(year);
    }

    public String getRelevance() {
        return relevance;
    }

    public String getAvailability() {
        return availability;
    }

    public List<String> getTopics() {
        return topics;
    }

    public List<String> getLayers() {
        return layers;
    }

    public List<String> getMethods() {
        return methods;
    }

    /** Selected IDs of one facet, by parameter name. */
    public List<String> selected(String facetKey) {
        return switch (facetKey) {
            case FacetIndex.TOPIC -> topics;
            case FacetIndex.LAYER -> layers;
            case FacetIndex.METHOD -> methods;
            default -> List.of();
        };
    }

    public String getMatch() {
        return match;
    }

    public boolean isMatchAll() {
        return MATCH_ALL.equals(match);
    }

    public int getKeywordCount() {
        return topics.size() + layers.size() + methods.size();
    }

    public int getPage() {
        return page;
    }

    public boolean isFiltered() {
        return !query.isBlank()
                || !category.isBlank()
                || !venue.isBlank()
                || year != null
                || !relevance.isBlank()
                || !availability.isBlank()
                || getKeywordCount() > 0;
    }
}

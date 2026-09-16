package org.systemsphd.explorer.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.systemsphd.explorer.catalog.Taxonomy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Paper {
    private static final int MAX_FULL_AUTHOR_LIST = 8;
    private static final int LEADING_AUTHORS_WHEN_TRUNCATED = 6;
    private static final int MAX_TOPIC_LABELS = 2;

    private final String id;
    private final String title;
    private final String venue;
    private final int year;
    private final String track;
    private final List<String> authors;
    private final String abstractText;
    private final String takeaway;
    private final PaperSummary summary;
    private final PaperReview review;
    private final String relevanceClass;
    private final List<String> topics;
    private final List<String> topicLabels;
    private final List<String> methods;
    private final List<String> systemLayers;
    private final List<String> institutions;
    private final List<String> locations;
    private final List<String> faculty;
    private final List<String> groups;
    private final boolean hasCode;
    private final boolean hasArtifact;
    private final String officialPage;
    private final String officialPdf;
    private final String codeUrl;
    private final String artifactUrl;
    private final String searchText;
    private final String titleSearchText;
    private final String abstractSearchText;

    private Paper(
            String id,
            String title,
            String venue,
            int year,
            String track,
            List<String> authors,
            String abstractText,
            String takeaway,
            PaperSummary summary,
            PaperReview review,
            String relevanceClass,
            List<String> topics,
            List<String> topicLabels,
            List<String> methods,
            List<String> systemLayers,
            List<String> institutions,
            List<String> locations,
            List<String> faculty,
            List<String> groups,
            boolean hasCode,
            boolean hasArtifact,
            String officialPage,
            String officialPdf,
            String codeUrl,
            String artifactUrl
    ) {
        this.id = id;
        this.title = title;
        this.venue = venue;
        this.year = year;
        this.track = track;
        this.authors = List.copyOf(authors);
        this.abstractText = abstractText;
        this.takeaway = takeaway;
        this.summary = summary;
        this.review = review;
        this.relevanceClass = relevanceClass;
        this.topics = List.copyOf(topics);
        this.topicLabels = List.copyOf(topicLabels);
        this.methods = List.copyOf(methods);
        this.systemLayers = List.copyOf(systemLayers);
        this.institutions = List.copyOf(institutions);
        this.locations = List.copyOf(locations);
        this.faculty = List.copyOf(faculty);
        this.groups = List.copyOf(groups);
        this.hasCode = hasCode;
        this.hasArtifact = hasArtifact;
        this.officialPage = officialPage;
        this.officialPdf = officialPdf;
        this.codeUrl = codeUrl;
        this.artifactUrl = artifactUrl;
        this.titleSearchText = title.toLowerCase(Locale.ROOT);
        this.abstractSearchText = abstractText.toLowerCase(Locale.ROOT);
        this.searchText = String.join(" ", List.of(
                title,
                String.join(" ", authors),
                abstractText,
                takeaway,
                summary == null ? "" : summary.searchText(),
                String.join(" ", topics),
                String.join(" ", methods),
                String.join(" ", systemLayers),
                String.join(" ", institutions),
                String.join(" ", locations),
                String.join(" ", faculty),
                String.join(" ", groups)
        )).toLowerCase(Locale.ROOT);
    }

    public static Paper from(JsonNode node, Taxonomy labels) {
        JsonNode links = node.path("links");
        List<String> faculty = stringList(node.path("faculty_ids"));
        if (faculty.isEmpty()) {
            faculty = namedObjectList(node.path("faculty"));
        }
        List<String> groups = stringList(node.path("group_ids"));
        if (groups.isEmpty()) {
            groups = namedObjectList(node.path("groups"));
        }
        String codeUrl = httpUrl(links, "code");
        String artifactUrl = httpUrl(links, "artifact");
        List<String> topics = stringList(node.path("topics"));
        List<String> topicLabels = topics.stream().limit(MAX_TOPIC_LABELS).map(labels::labelFor).toList();
        return new Paper(
                text(node, "id"),
                text(node, "title"),
                text(node, "venue"),
                node.path("year").asInt(),
                text(node, "track_or_session"),
                stringList(node.path("authors")),
                text(node, "abstract"),
                text(node, "takeaway"),
                PaperSummary.from(node.path("summary")),
                PaperReview.from(node.path("review")),
                text(node, "relevance_class"),
                topics,
                topicLabels,
                stringList(node.path("methods")),
                stringList(node.path("system_layers")),
                stringList(node.path("institutions")),
                stringList(node.path("locations")),
                faculty,
                groups,
                node.path("has_code").asBoolean(false) || codeUrl != null,
                node.path("has_artifact").asBoolean(false) || artifactUrl != null,
                httpUrl(links, "official_page"),
                httpUrl(links, "official_pdf"),
                codeUrl,
                artifactUrl
        );
    }

    /**
     * Returns the link only when it is an absolute http or https URL, so that
     * nothing else can reach an href attribute in the rendered page.
     */
    static String httpUrl(JsonNode node, String field) {
        String value = nullableText(node, field);
        if (value == null) {
            return null;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return null;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            return scheme.equals("http") || scheme.equals("https") ? value : null;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        String value = nullableText(node, field);
        return value == null ? "" : value;
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private static List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private static List<String> namedObjectList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            String value = text(item, "name");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getVenue() {
        return venue;
    }

    public int getYear() {
        return year;
    }

    /** Program track or session name as published, or empty when unknown. */
    public String getTrack() {
        return track;
    }

    public List<String> getAuthors() {
        return authors;
    }

    /**
     * Long author lists keep the leading authors and the last author, since the
     * last author of a systems paper is usually the supervising faculty member.
     */
    public String getAuthorLine() {
        if (authors.size() <= MAX_FULL_AUTHOR_LIST) {
            return String.join(", ", authors);
        }
        int hidden = authors.size() - LEADING_AUTHORS_WHEN_TRUNCATED - 1;
        return String.join(", ", authors.subList(0, LEADING_AUTHORS_WHEN_TRUNCATED))
                + ", … " + hidden + " more …, "
                + authors.get(authors.size() - 1);
    }

    public List<String> getInstitutions() {
        return institutions;
    }

    /** Institutions joined for one line, e.g. "MIT, USA · ETH Zurich, Switzerland". */
    public String getInstitutionLine() {
        return String.join(" \u00b7 ", institutions);
    }

    public boolean isHasInstitutions() {
        return !institutions.isEmpty();
    }

    /** The published abstract, or empty when it has not been collected. */
    public String getAbstractText() {
        return abstractText;
    }

    public boolean isHasAbstract() {
        return !abstractText.isBlank();
    }

    public String getTakeaway() {
        return takeaway;
    }

    /** LLM-written story, or null when the summary stage has not started. */
    public PaperSummary getSummary() {
        return summary;
    }

    public boolean isHasSummary() {
        return summary != null;
    }

    /** Independent review verdict, or null when no review has started. */
    public PaperReview getReview() {
        return review;
    }

    public boolean isHasReview() {
        return review != null;
    }

    public boolean isHasTakeaway() {
        return !takeaway.isBlank();
    }

    public String getRelevanceClass() {
        return relevanceClass;
    }

    public List<String> getTopics() {
        return topics;
    }

    /** At most two display labels, in the classifier's order. */
    public List<String> getTopicLabels() {
        return topicLabels;
    }

    public List<String> getMethods() {
        return methods;
    }

    public List<String> getSystemLayers() {
        return systemLayers;
    }

    public boolean isHasCode() {
        return hasCode;
    }

    public boolean isHasArtifact() {
        return hasArtifact;
    }

    public String getOfficialPage() {
        return officialPage;
    }

    public String getOfficialPdf() {
        return officialPdf;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public String getArtifactUrl() {
        return artifactUrl;
    }

    public boolean isHasOfficialPage() {
        return officialPage != null;
    }

    public boolean isHasOfficialPdf() {
        return officialPdf != null;
    }

    public boolean isHasCodeUrl() {
        return codeUrl != null;
    }

    public boolean isHasArtifactUrl() {
        return artifactUrl != null;
    }

    public boolean isHasLinks() {
        return officialPage != null || officialPdf != null || codeUrl != null || artifactUrl != null;
    }

    public String getSearchText() {
        return searchText;
    }

    public String getTitleSearchText() {
        return titleSearchText;
    }

    public String getAbstractSearchText() {
        return abstractSearchText;
    }

    public String getRelevanceLabel() {
        return relevanceLabel(relevanceClass);
    }

    /** Core-systems papers are the default expectation, so only other classes are tagged. */
    public boolean isShowRelevanceTag() {
        return !"CORE_SYSTEMS".equals(relevanceClass) && !relevanceClass.isBlank();
    }

    public static String relevanceLabel(String relevanceClass) {
        return switch (relevanceClass) {
            case "CORE_SYSTEMS" -> "Core systems";
            case "SYSTEMS_ADJACENT" -> "Systems-adjacent";
            case "NEEDS_HUMAN_REVIEW" -> "Needs review";
            case "EXCLUDED" -> "Excluded";
            default -> Taxonomy.humanize(relevanceClass);
        };
    }
}

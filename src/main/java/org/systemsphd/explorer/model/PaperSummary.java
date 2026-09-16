package org.systemsphd.explorer.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.systemsphd.explorer.catalog.Taxonomy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The LLM-written beginner story for one paper, as exported from
 * {@code summary.json}. Absent when the summary stage has not started.
 */
public final class PaperSummary {
    /** One reported result together with where in the paper it was read. */
    public static final class Result {
        private final String text;
        private final String locator;

        Result(String text, String locator) {
            this.text = text;
            this.locator = locator;
        }

        public String getText() {
            return text;
        }

        public String getLocator() {
            return locator;
        }

        public boolean isHasLocator() {
            return !locator.isBlank();
        }
    }

    private final String status;
    private final String writtenBy;
    private final String updatedAt;
    private final String confidence;
    private final String background;
    private final String villain;
    private final String approach;
    private final String impact;
    private final String evaluationOverview;
    private final String implemented;
    private final String setting;
    private final List<String> baselines;
    private final List<Result> results;
    private final List<String> studyTypes;
    private final String limitations;
    private final String phdGroupSignal;
    private final List<String> beginnerConcepts;
    private final Map<String, String> readingCoverage;

    private PaperSummary(JsonNode node) {
        JsonNode evaluation = node.path("evaluation");
        status = text(node, "status");
        writtenBy = text(node, "written_by");
        updatedAt = text(node, "updated_at");
        confidence = text(node, "confidence");
        background = text(node, "background");
        villain = text(node, "villain");
        approach = text(node, "approach");
        impact = text(node, "impact");
        evaluationOverview = text(evaluation, "overview");
        implemented = text(evaluation, "implemented");
        setting = text(evaluation, "setting");
        baselines = strings(evaluation.path("baselines"));
        List<Result> parsedResults = new ArrayList<>();
        for (JsonNode result : evaluation.path("results")) {
            String resultText = text(result, "text");
            if (!resultText.isBlank()) {
                parsedResults.add(new Result(resultText, text(result, "locator")));
            }
        }
        results = List.copyOf(parsedResults);
        studyTypes = strings(evaluation.path("study_types")).stream().map(Taxonomy::humanize).toList();
        limitations = text(node, "limitations");
        phdGroupSignal = text(node, "phd_group_signal");
        beginnerConcepts = strings(node.path("beginner_concepts"));
        Map<String, String> coverage = new LinkedHashMap<>();
        node.path("reading_coverage").fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                coverage.put(Taxonomy.humanize(entry.getKey()), Taxonomy.humanize(entry.getValue().asText()));
            }
        });
        readingCoverage = Map.copyOf(coverage);
    }

    /** Returns null unless the node is an object with at least a status. */
    static PaperSummary from(JsonNode node) {
        if (node == null || !node.isObject() || text(node, "status").isBlank()) {
            return null;
        }
        return new PaperSummary(node);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText().trim() : "";
    }

    private static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        }
        return List.copyOf(values);
    }

    /** Text searched by the query box: the whole story, without provenance noise. */
    String searchText() {
        List<String> parts = new ArrayList<>(List.of(
                background, villain, approach, impact, evaluationOverview, implemented, setting,
                limitations, phdGroupSignal, String.join(" ", baselines), String.join(" ", beginnerConcepts)
        ));
        results.forEach(result -> parts.add(result.getText()));
        return String.join(" ", parts).toLowerCase(Locale.ROOT);
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return switch (status) {
            case "APPROVED" -> "Approved by independent review";
            case "READY_FOR_REVIEW" -> "Ready for review";
            case "CHANGES_REQUESTED" -> "Changes requested by reviewer";
            case "IN_PROGRESS" -> "In progress";
            case "BLOCKED" -> "Blocked";
            default -> Taxonomy.humanize(status);
        };
    }

    /** True until an independent reviewer has approved the text. */
    public boolean isUnreviewed() {
        return !"APPROVED".equals(status);
    }

    public String getWrittenBy() {
        return writtenBy;
    }

    /** Date part of the last update timestamp. */
    public String getUpdatedDate() {
        return updatedAt.length() >= 10 ? updatedAt.substring(0, 10) : updatedAt;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getConfidenceLabel() {
        return Taxonomy.humanize(confidence);
    }

    public String getBackground() {
        return background;
    }

    public String getVillain() {
        return villain;
    }

    public String getApproach() {
        return approach;
    }

    public String getImpact() {
        return impact;
    }

    public String getEvaluationOverview() {
        return evaluationOverview;
    }

    public String getImplemented() {
        return implemented;
    }

    public String getSetting() {
        return setting;
    }

    public List<String> getBaselines() {
        return baselines;
    }

    public List<Result> getResults() {
        return results;
    }

    public List<String> getStudyTypes() {
        return studyTypes;
    }

    public String getLimitations() {
        return limitations;
    }

    public String getPhdGroupSignal() {
        return phdGroupSignal;
    }

    public List<String> getBeginnerConcepts() {
        return beginnerConcepts;
    }

    public Map<String, String> getReadingCoverage() {
        return readingCoverage;
    }

    public boolean isHasEvaluation() {
        return !evaluationOverview.isBlank() || !implemented.isBlank() || !setting.isBlank()
                || !baselines.isEmpty() || !results.isEmpty();
    }
}

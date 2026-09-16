package org.systemsphd.explorer.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.systemsphd.explorer.catalog.Taxonomy;

import java.util.ArrayList;
import java.util.List;

/**
 * The independent reviewer's verdict on a paper bundle, as exported from
 * {@code review.json}. Absent when no review has started.
 */
public final class PaperReview {
    public static final class Issue {
        private final String severity;
        private final String stage;
        private final String description;
        private final String status;

        Issue(String severity, String stage, String description, String status) {
            this.severity = severity;
            this.stage = stage;
            this.description = description;
            this.status = status;
        }

        public String getSeverity() {
            return severity;
        }

        public String getStage() {
            return stage;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }
    }

    private final String status;
    private final String decision;
    private final String reviewer;
    private final String updatedAt;
    private final List<Issue> issues;

    private PaperReview(JsonNode node) {
        status = text(node, "status");
        decision = text(node, "decision");
        reviewer = text(node, "reviewer");
        updatedAt = text(node, "updated_at");
        List<Issue> parsed = new ArrayList<>();
        for (JsonNode issue : node.path("issues")) {
            String description = text(issue, "description");
            if (!description.isBlank()) {
                parsed.add(new Issue(
                        text(issue, "severity"), text(issue, "stage"), description, text(issue, "status")
                ));
            }
        }
        issues = List.copyOf(parsed);
    }

    static PaperReview from(JsonNode node) {
        if (node == null || !node.isObject() || text(node, "decision").isBlank()) {
            return null;
        }
        return new PaperReview(node);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText().trim() : "";
    }

    public String getStatus() {
        return status;
    }

    public String getDecision() {
        return decision;
    }

    public String getDecisionLabel() {
        return switch (decision) {
            case "APPROVED" -> "Approved";
            case "CHANGES_REQUESTED" -> "Changes requested";
            case "BLOCKED" -> "Blocked";
            case "NOT_REVIEWED" -> "Not reviewed yet";
            default -> Taxonomy.humanize(decision);
        };
    }

    public String getReviewer() {
        return reviewer;
    }

    public String getUpdatedDate() {
        return updatedAt.length() >= 10 ? updatedAt.substring(0, 10) : updatedAt;
    }

    public List<Issue> getIssues() {
        return issues;
    }
}

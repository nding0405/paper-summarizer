package org.systemsphd.explorer.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TreeSet;

/**
 * Builds the two files the web application reads:
 *
 * <ul>
 *   <li>{@code papers.json} — one compact card per paper bundle, and</li>
 *   <li>{@code venue-years.json} — the coverage manifest, i.e. the committed
 *       baseline manifest with each shard's collection status filled in.</li>
 * </ul>
 *
 * The shards are validated first; an invalid corpus is never exported.
 */
public final class ShardExport {
    private ShardExport() {
    }

    static int run(List<String> arguments) throws IOException {
        Main.Options options = new Main.Options(arguments);
        Path root = Path.of(options.require("root")).toAbsolutePath().normalize();
        Path output = Path.of(options.require("output")).toAbsolutePath().normalize();
        Path repository = Shards.repositoryRoot(options.get("repo", null));

        Validator.Report report = Validator.validate(root, repository);
        report.errors().forEach(System.err::println);
        if (!report.ok()) {
            System.err.println("Cannot export invalid shards: " + report.summary());
            return 1;
        }

        Files.createDirectories(output);
        Path papers = output.resolve("papers.json");
        Path manifest = output.resolve("venue-years.json");
        JsonFiles.write(papers, papersExport(root));
        JsonFiles.write(manifest, manifest(root, repository));
        System.out.println(papers);
        System.out.println(manifest);
        return 0;
    }

    /** The {@code papers.json} payload for every bundle under the root. */
    public static ObjectNode papersExport(Path root) throws IOException {
        ObjectNode payload = JsonFiles.mapper().createObjectNode();
        payload.put("schema_version", "1.0.0");
        payload.put("is_fixture_export", false);
        ArrayNode papers = payload.putArray("papers");
        for (Path directory : Shards.paperDirectories(root)) {
            papers.add(paperCard(directory));
        }
        return payload;
    }

    /** One paper's card, in the same shape the old Python export produced. */
    static ObjectNode paperCard(Path directory) throws IOException {
        ObjectNode metadata = JsonFiles.readObject(directory.resolve("metadata.json"));
        ObjectNode relevance = JsonFiles.readObject(directory.resolve("relevance.json"));
        ObjectNode summary = JsonFiles.readObject(directory.resolve("summary.json"));
        ObjectNode groups = JsonFiles.readObject(directory.resolve("groups.json"));
        ObjectNode awards = JsonFiles.readObject(directory.resolve("awards.json"));
        ObjectNode review = JsonFiles.readObject(directory.resolve("review.json"));

        TreeSet<String> institutions = new TreeSet<>();
        TreeSet<String> locations = new TreeSet<>();
        for (JsonNode author : metadata.path("authors")) {
            for (JsonNode affiliation : author.path("affiliations")) {
                String name = text(affiliation, "normalized_name");
                if (name.isEmpty()) {
                    name = text(affiliation, "name_as_published");
                }
                if (!name.isEmpty()) {
                    institutions.add(name);
                }
                String city = text(affiliation, "city");
                String country = text(affiliation, "country");
                String location = String.join(", ", nonEmpty(city, country));
                if (!location.isEmpty()) {
                    locations.add(location);
                }
            }
        }

        ObjectNode card = JsonFiles.mapper().createObjectNode();
        card.set("id", metadata.path("paper_id"));
        card.set("title", metadata.path("title"));
        card.set("venue", metadata.path("venue"));
        card.set("year", metadata.path("year"));
        card.set("track_or_session", metadata.path("track_or_session"));
        ArrayNode authors = card.putArray("authors");
        for (JsonNode author : metadata.path("authors")) {
            authors.add(author.path("name"));
        }
        card.set("abstract", metadata.path("abstract"));
        card.set("takeaway", summary.path("takeaway"));
        card.set("summary", summaryCard(summary));
        card.set("review", reviewCard(review));
        card.set("relevance_class", relevance.path("class"));
        card.set("relevance_score", relevance.path("score"));
        card.set("topics", relevance.path("topics"));
        card.set("system_layers", relevance.path("system_layers"));
        card.set("methods", relevance.path("methods"));
        ArrayNode institutionNode = card.putArray("institutions");
        institutions.forEach(institutionNode::add);
        ArrayNode locationNode = card.putArray("locations");
        locations.forEach(locationNode::add);
        TreeSet<String> facultyIds = new TreeSet<>();
        TreeSet<String> groupIds = new TreeSet<>();
        for (JsonNode association : groups.path("associations")) {
            String facultyId = text(association, "faculty_id");
            if (!facultyId.isEmpty()) {
                facultyIds.add(facultyId);
            }
            String groupId = text(association, "group_id");
            if (!groupId.isEmpty()) {
                groupIds.add(groupId);
            }
        }
        ArrayNode facultyNode = card.putArray("faculty_ids");
        facultyIds.forEach(facultyNode::add);
        ArrayNode groupNode = card.putArray("group_ids");
        groupIds.forEach(groupNode::add);
        ArrayNode awardNode = card.putArray("awards");
        for (JsonNode award : awards.path("awards")) {
            ObjectNode item = awardNode.addObject();
            item.set("name", award.path("official_name"));
            item.set("category", award.path("category"));
        }
        JsonNode links = metadata.path("links");
        card.put("has_code", !links.path("code").isNull() && !links.path("code").asText("").isEmpty());
        card.put("has_artifact", !links.path("artifact").isNull() && !links.path("artifact").asText("").isEmpty());
        ObjectNode linkNode = card.putObject("links");
        for (String key : List.of("official_page", "official_pdf", "code", "artifact")) {
            linkNode.set(key, links.path(key).isMissingNode() ? JsonFiles.mapper().nullNode() : links.path(key));
        }
        card.set("confidence", relevance.path("verification").path("confidence"));
        card.set("last_verified_at", metadata.path("verification").path("last_verified_at"));
        card.put("is_fixture", false);
        return card;
    }

    /** The beginner story for the interface, or null when nothing was written. */
    static JsonNode summaryCard(ObjectNode summary) {
        String status = text(summary.path("workflow"), "status");
        if ("NOT_STARTED".equals(status) && text(summary, "takeaway").isEmpty()) {
            return JsonFiles.mapper().nullNode();
        }
        JsonNode evaluation = summary.path("evaluation");
        ObjectNode card = JsonFiles.mapper().createObjectNode();
        card.put("status", status);
        card.set("written_by", orNull(summary.path("workflow").path("owner")));
        card.set("updated_at", orNull(summary.path("workflow").path("updated_at")));
        card.set("confidence", orNull(summary.path("verification").path("confidence")));
        for (String field : List.of("background", "villain", "approach", "impact")) {
            card.set(field, orNull(summary.path(field)));
        }
        ObjectNode evaluationCard = card.putObject("evaluation");
        for (String field : List.of("overview", "implemented", "setting")) {
            evaluationCard.set(field, orNull(evaluation.path(field)));
        }
        evaluationCard.set("baselines", arrayOrEmpty(evaluation.path("baselines")));
        ArrayNode results = evaluationCard.putArray("results");
        for (JsonNode result : evaluation.path("results")) {
            ObjectNode item = results.addObject();
            item.set("text", orNull(result.path("text")));
            item.set("locator", orNull(result.path("locator")));
        }
        evaluationCard.set("study_types", arrayOrEmpty(evaluation.path("study_types")));
        card.set("limitations", orNull(summary.path("limitations")));
        card.set("phd_group_signal", orNull(summary.path("phd_group_signal")));
        card.set("beginner_concepts", arrayOrEmpty(summary.path("beginner_concepts")));
        JsonNode coverage = summary.path("reading_coverage");
        card.set("reading_coverage", coverage.isObject() ? coverage : JsonFiles.mapper().createObjectNode());
        return card;
    }

    /** The independent reviewer's verdict, or null when no review has started. */
    static JsonNode reviewCard(ObjectNode review) {
        String status = text(review.path("workflow"), "status");
        if ("NOT_STARTED".equals(status) && "NOT_REVIEWED".equals(text(review, "decision"))) {
            return JsonFiles.mapper().nullNode();
        }
        ObjectNode card = JsonFiles.mapper().createObjectNode();
        card.put("status", status);
        card.set("decision", orNull(review.path("decision")));
        card.set("reviewer", orNull(review.path("reviewer")));
        card.set("updated_at", orNull(review.path("workflow").path("updated_at")));
        ArrayNode issues = card.putArray("issues");
        for (JsonNode issue : review.path("issues")) {
            ObjectNode item = issues.addObject();
            for (String field : List.of("severity", "stage", "description", "status")) {
                item.set(field, orNull(issue.path(field)));
            }
        }
        return card;
    }

    /** The coverage manifest: the committed baseline with live shard statuses. */
    public static ObjectNode manifest(Path root, Path repository) throws IOException {
        ObjectNode baseline = JsonFiles.readObject(repository.resolve("data/venue-year-manifest.json"));
        ObjectNode generated = baseline.deepCopy();
        generated.put("$schema", "https://systems-phd-explorer.local/schemas/v1/venue-year-manifest.schema.json");
        generated.put("generated_at", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
                .replaceAll("\\.\\d+Z$", "Z"));
        ArrayNode entries = generated.putArray("venue_years");
        for (JsonNode item : baseline.path("venue_years")) {
            ObjectNode entry = item.deepCopy();
            Path shard = root.resolve(item.path("venue").asText()).resolve(item.path("year").asText())
                    .resolve("venue-year.json");
            if (Files.exists(shard)) {
                ObjectNode venueYear = JsonFiles.readObject(shard);
                entry.set("workflow_status", venueYear.path("workflow").path("status"));
                entry.set("collection_status", venueYear.path("collection_status"));
                entry.set("last_verified_at", orNull(venueYear.path("verification").path("last_verified_at")));
            }
            entries.add(entry);
        }
        return generated;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : "";
    }

    private static List<String> nonEmpty(String... values) {
        return java.util.Arrays.stream(values).filter(value -> value != null && !value.isEmpty()).toList();
    }

    private static JsonNode orNull(JsonNode node) {
        return node.isMissingNode() ? JsonFiles.mapper().nullNode() : node;
    }

    private static JsonNode arrayOrEmpty(JsonNode node) {
        return node.isArray() ? node : JsonFiles.mapper().createArrayNode();
    }

}

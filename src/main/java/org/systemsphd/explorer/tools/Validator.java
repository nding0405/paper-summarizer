package org.systemsphd.explorer.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Validates the agent-authored data: every stage file, venue-year file, and
 * entity file against its JSON Schema, plus the cross-file rules the merge
 * depends on (paths match IDs, IDs are unique, source references resolve,
 * taxonomy values exist, entity references exist). This is the former
 * "draft" profile; release gating is not checked.
 */
public final class Validator {
    private static final String SCHEMA_BASE = "https://systems-phd-explorer.local/schemas/v1/";
    private static final Set<Integer> SUPPORTED_YEARS = Set.of(2023, 2024, 2025, 2026);
    private static final Pattern DOI = Pattern.compile("10\\.\\d{4,9}/\\S+");
    private static final Map<String, String> STAGE_SCHEMAS = Map.of(
            "metadata.json", "paper-metadata.schema.json",
            "relevance.json", "relevance.schema.json",
            "summary.json", "summary.schema.json",
            "groups.json", "groups.schema.json",
            "awards.json", "awards.schema.json",
            "review.json", "review.schema.json"
    );
    private static final Map<String, String[]> ENTITY_SCHEMAS = Map.of(
            "authors", new String[] {"author.schema.json", "author_id"},
            "faculty", new String[] {"faculty.schema.json", "faculty_id"},
            "research-groups", new String[] {"research-group.schema.json", "group_id"},
            "institutions", new String[] {"institution.schema.json", "institution_id"}
    );

    /** Collected problems; {@code ok()} is true when there are no errors. */
    public static final class Report {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        int bundles;
        int files;

        void error(Path path, String message) {
            errors.add("ERROR: " + path + ": " + message);
        }

        void warning(Path path, String message) {
            warnings.add("WARNING: " + path + ": " + message);
        }

        public List<String> errors() {
            return errors;
        }

        public List<String> warnings() {
            return warnings;
        }

        public boolean ok() {
            return errors.isEmpty();
        }

        public String summary() {
            return String.format("Checked %d paper bundle(s) and %d JSON file(s): %d error(s), %d warning(s).",
                    bundles, files, errors.size(), warnings.size());
        }
    }

    private final Path repository;
    private final JsonSchemaFactory factory;
    private final Map<String, JsonSchema> schemas = new HashMap<>();

    private Validator(Path repository) {
        this.repository = repository;
        String local = repository.resolve("schemas/v1").toUri().toString();
        this.factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaMappers(mappers -> mappers.mapPrefix(SCHEMA_BASE, local)));
    }

    static int run(List<String> arguments) throws IOException {
        Main.Options options = new Main.Options(arguments);
        Path root = Path.of(options.require("root")).toAbsolutePath().normalize();
        Report report = validate(root, Shards.repositoryRoot(options.get("repo", null)));
        report.errors().forEach(System.out::println);
        report.warnings().forEach(System.out::println);
        System.out.println(report.summary());
        return report.ok() ? 0 : 1;
    }

    public static Report validate(Path shardsRoot, Path repository) throws IOException {
        Validator validator = new Validator(repository);
        Report report = new Report();
        validator.validateShards(shardsRoot, report);
        validator.validateEntities(report);
        return report;
    }

    private JsonSchema schema(String filename) {
        return schemas.computeIfAbsent(filename, name -> factory.getSchema(
                SchemaLocation.of(SCHEMA_BASE + name),
                SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build()));
    }

    private boolean checkSchema(Path path, JsonNode payload, String schemaName, Report report) {
        Set<ValidationMessage> messages = schema(schemaName).validate(payload);
        for (ValidationMessage message : messages) {
            report.error(path, message.getMessage());
        }
        return messages.isEmpty();
    }

    private ObjectNode load(Path path, Report report) {
        report.files++;
        try {
            return JsonFiles.readObject(path);
        } catch (IOException exception) {
            report.error(path, exception.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Shards
    // ------------------------------------------------------------------

    private void validateShards(Path root, Report report) throws IOException {
        ObjectNode venuesDocument = JsonFiles.readObject(repository.resolve("config/venues.json"));
        ObjectNode taxonomy = JsonFiles.readObject(repository.resolve("config/taxonomy.v1.json"));
        Set<String> venues = new HashSet<>();
        venuesDocument.path("venues").forEach(venue -> venues.add(venue.path("slug").asText()));
        Set<String> topics = new HashSet<>();
        for (JsonNode track : taxonomy.path("tracks")) {
            topics.add(track.path("id").asText());
            track.path("subtopics").forEach(subtopic -> topics.add(subtopic.asText()));
        }
        Set<String> layers = strings(taxonomy.path("system_layers"));
        Set<String> contributions = strings(taxonomy.path("contribution_types"));
        Set<String> methods = strings(taxonomy.path("methods"));
        Map<String, Set<String>> entityIds = new HashMap<>();
        for (String directory : ENTITY_SCHEMAS.keySet()) {
            entityIds.put(directory, entityIdsIn(directory));
        }

        Set<String> venueYears = new HashSet<>();
        for (Path path : Shards.venueYearFiles(root)) {
            ObjectNode payload = load(path, report);
            if (payload == null || !checkSchema(path, payload, "venue-year.schema.json", report)) {
                continue;
            }
            String venue = payload.path("venue").asText();
            if (!venues.contains(venue)) {
                report.error(path, "unknown venue '" + venue + "'");
            }
            String key = venue + "/" + payload.path("year").asText();
            if (!venueYears.add(key)) {
                report.error(path, "duplicate venue-year record " + key);
            }
        }

        Map<String, Path> seenIds = new HashMap<>();
        Map<String, Path> seenTitles = new HashMap<>();
        Map<String, Map.Entry<JsonNode, Path>> globalSources = new HashMap<>();
        for (Path directory : Shards.paperDirectories(root)) {
            report.bundles++;
            List<String> missing = new ArrayList<>();
            for (String filename : Shards.STAGE_FILES) {
                if (!Files.exists(directory.resolve(filename))) {
                    missing.add(filename);
                }
            }
            if (!missing.isEmpty()) {
                report.error(directory, "incomplete paper bundle; missing " + missing);
                continue;
            }
            Map<String, ObjectNode> bundle = new LinkedHashMap<>();
            boolean failed = false;
            for (String filename : Shards.STAGE_FILES) {
                Path path = directory.resolve(filename);
                ObjectNode payload = load(path, report);
                if (payload == null || !checkSchema(path, payload, STAGE_SCHEMAS.get(filename), report)) {
                    failed = true;
                    continue;
                }
                bundle.put(filename, payload);
                Set<String> stageSources = new HashSet<>();
                for (JsonNode source : payload.path("sources")) {
                    String id = source.path("id").asText();
                    if (!stageSources.add(id)) {
                        report.error(path, "duplicate source ID within stage: " + id);
                    }
                    Map.Entry<JsonNode, Path> prior = globalSources.putIfAbsent(id, Map.entry(source, path));
                    if (prior != null && !prior.getKey().equals(source)) {
                        report.error(path, "source ID '" + id + "' has different content in " + prior.getValue());
                    }
                }
                checkSourceReferences(path, filename, payload, stageSources, report);
            }
            if (failed) {
                continue;
            }

            ObjectNode metadata = bundle.get("metadata.json");
            String recordId = metadata.path("paper_id").asText();
            if (!recordId.equals(directory.getFileName().toString())) {
                report.error(directory.resolve("metadata.json"), "paper_id '" + recordId + "' does not match the directory name");
            }
            for (Map.Entry<String, ObjectNode> stage : bundle.entrySet()) {
                if (!stage.getValue().path("paper_id").asText().equals(recordId)) {
                    report.error(directory.resolve(stage.getKey()), "paper_id does not match '" + recordId + "'");
                }
            }
            Path relative = root.relativize(directory);
            if (relative.getNameCount() != 4 || !relative.getName(2).toString().equals("papers")) {
                report.error(directory, "paper path must be <venue>/<year>/papers/<id>");
                continue;
            }
            String pathVenue = relative.getName(0).toString();
            int pathYear;
            try {
                pathYear = Integer.parseInt(relative.getName(1).toString());
            } catch (NumberFormatException exception) {
                report.error(directory, "paper path must be <venue>/<year>/papers/<id>");
                continue;
            }
            if (!metadata.path("venue").asText().equals(pathVenue) || metadata.path("year").asInt() != pathYear) {
                report.error(directory.resolve("metadata.json"), "venue/year do not match the shard path");
            }
            if (!venues.contains(pathVenue) || !SUPPORTED_YEARS.contains(pathYear)) {
                report.error(directory, "paper is outside the configured venue/year scope");
            }
            if (!venueYears.contains(pathVenue + "/" + pathYear)) {
                report.error(directory, "paper shard lacks venue-year.json");
            }
            Path prior = seenIds.putIfAbsent(recordId, directory);
            if (prior != null) {
                report.error(directory.resolve("metadata.json"), "duplicate paper ID also in " + prior);
            }
            String titleKey = pathVenue + "/" + pathYear + "/" + metadata.path("title").asText().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
            Path priorTitle = seenTitles.putIfAbsent(titleKey, directory);
            if (priorTitle != null) {
                report.warning(directory.resolve("metadata.json"), "same title as " + priorTitle);
            }
            JsonNode doi = metadata.path("links").path("doi");
            if (doi.isTextual() && !DOI.matcher(doi.asText().replaceFirst("^https://doi\\.org/", "")).matches()) {
                report.error(directory.resolve("metadata.json"), "invalid DOI format: " + doi.asText());
            }
            List<Integer> positions = new ArrayList<>();
            metadata.path("authors").forEach(author -> positions.add(author.path("position").asInt()));
            List<Integer> expected = new ArrayList<>();
            for (int index = 1; index <= positions.size(); index++) {
                expected.add(index);
            }
            if (!positions.isEmpty() && !new TreeSet<>(positions).equals(new TreeSet<>(expected))) {
                report.error(directory.resolve("metadata.json"), "author positions must be consecutive from 1");
            }

            ObjectNode relevance = bundle.get("relevance.json");
            unknown(directory.resolve("relevance.json"), "topics", relevance.path("topics"), topics, report);
            unknown(directory.resolve("relevance.json"), "system layers", relevance.path("system_layers"), layers, report);
            unknown(directory.resolve("relevance.json"), "contribution types", relevance.path("contribution_types"), contributions, report);
            unknown(directory.resolve("relevance.json"), "methods", relevance.path("methods"), methods, report);

            for (JsonNode association : bundle.get("groups.json").path("associations")) {
                checkEntity(directory.resolve("groups.json"), association, "faculty_id", "faculty", entityIds, report);
                checkEntity(directory.resolve("groups.json"), association, "group_id", "research-groups", entityIds, report);
                checkEntity(directory.resolve("groups.json"), association, "publication_institution_id", "institutions", entityIds, report);
                checkEntity(directory.resolve("groups.json"), association, "current_institution_id", "institutions", entityIds, report);
            }
        }
    }

    private static void checkSourceReferences(Path path, String filename, JsonNode payload, Set<String> sources, Report report) {
        List<Map.Entry<String, String>> referenced = new ArrayList<>();
        switch (filename) {
            case "metadata.json" -> payload.path("authors").forEach(author -> author.path("affiliations").forEach(
                    affiliation -> affiliation.path("source_ids").forEach(id -> referenced.add(Map.entry(id.asText(), "affiliation")))));
            case "summary.json" -> {
                payload.path("claims").forEach(claim -> claim.path("source_ids").forEach(
                        id -> referenced.add(Map.entry(id.asText(), "claim " + claim.path("id").asText()))));
                payload.path("evaluation").path("results").forEach(result -> result.path("source_ids").forEach(
                        id -> referenced.add(Map.entry(id.asText(), "evaluation result " + result.path("id").asText()))));
            }
            case "groups.json" -> payload.path("associations").forEach(association -> association.path("evidence_source_ids")
                    .forEach(id -> referenced.add(Map.entry(id.asText(), "group association"))));
            case "awards.json" -> payload.path("awards").forEach(award -> {
                if (award.path("official_source_id").isTextual()) {
                    referenced.add(Map.entry(award.path("official_source_id").asText(), "award " + award.path("award_id").asText()));
                }
            });
            default -> {
            }
        }
        for (Map.Entry<String, String> reference : referenced) {
            if (!sources.contains(reference.getKey())) {
                report.error(path, reference.getValue() + " references missing stage source '" + reference.getKey() + "'");
            }
        }
    }

    private static void unknown(Path path, String label, JsonNode values, Set<String> known, Report report) {
        List<String> unknown = new ArrayList<>();
        for (JsonNode value : values) {
            if (!known.contains(value.asText())) {
                unknown.add(value.asText());
            }
        }
        if (!unknown.isEmpty()) {
            report.error(path, "unknown " + label + ": " + unknown);
        }
    }

    private static void checkEntity(Path path, JsonNode association, String field, String directory,
                                    Map<String, Set<String>> entityIds, Report report) {
        JsonNode value = association.path(field);
        if (value.isTextual() && !value.asText().isEmpty() && !entityIds.get(directory).contains(value.asText())) {
            report.error(path, field + " references missing " + directory + " entity '" + value.asText() + "'");
        }
    }

    // ------------------------------------------------------------------
    // Entities
    // ------------------------------------------------------------------

    private Set<String> entityIdsIn(String directory) throws IOException {
        Set<String> ids = new HashSet<>();
        Path folder = repository.resolve("data/entities").resolve(directory);
        if (!Files.isDirectory(folder)) {
            return ids;
        }
        String idField = ENTITY_SCHEMAS.get(directory)[1];
        try (Stream<Path> files = Files.list(folder)) {
            for (Path path : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                try {
                    ids.add(JsonFiles.readObject(path).path(idField).asText());
                } catch (IOException ignored) {
                    // Reported when the entity is validated.
                }
            }
        }
        return ids;
    }

    private void validateEntities(Report report) throws IOException {
        Map<String, Set<String>> ids = new HashMap<>();
        Map<String, List<Map.Entry<Path, ObjectNode>>> payloads = new HashMap<>();
        for (Map.Entry<String, String[]> entry : ENTITY_SCHEMAS.entrySet()) {
            String directory = entry.getKey();
            Path folder = repository.resolve("data/entities").resolve(directory);
            ids.put(directory, new HashSet<>());
            payloads.put(directory, new ArrayList<>());
            if (!Files.isDirectory(folder)) {
                continue;
            }
            List<Path> files;
            try (Stream<Path> stream = Files.list(folder)) {
                files = stream.filter(path -> path.toString().endsWith(".json")).sorted().toList();
            }
            for (Path path : files) {
                ObjectNode payload = load(path, report);
                if (payload == null || !checkSchema(path, payload, entry.getValue()[0], report)) {
                    continue;
                }
                String id = payload.path(entry.getValue()[1]).asText();
                if (!ids.get(directory).add(id)) {
                    report.error(path, "duplicate entity ID '" + id + "'");
                }
                payloads.get(directory).add(Map.entry(path, payload));
            }
        }
        for (Map.Entry<Path, ObjectNode> faculty : payloads.get("faculty")) {
            String authorId = faculty.getValue().path("author_id").asText();
            if (!ids.get("authors").contains(authorId)) {
                report.error(faculty.getKey(), "faculty references missing author '" + authorId + "'");
            }
        }
        for (Map.Entry<Path, ObjectNode> group : payloads.get("research-groups")) {
            JsonNode institution = group.getValue().path("institution_id");
            if (institution.isTextual() && !ids.get("institutions").contains(institution.asText())) {
                report.error(group.getKey(), "group references missing institution '" + institution.asText() + "'");
            }
            for (JsonNode facultyId : group.getValue().path("faculty_ids")) {
                if (!ids.get("faculty").contains(facultyId.asText())) {
                    report.error(group.getKey(), "group references missing faculty '" + facultyId.asText() + "'");
                }
            }
        }
    }

    private static Set<String> strings(JsonNode array) {
        Set<String> values = new HashSet<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }
}

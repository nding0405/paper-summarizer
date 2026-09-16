package org.systemsphd.explorer.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Export, validation, and JSON formatting against the committed release-profile fixture. */
class ShardToolsTest {
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();
    private static final Path FIXTURE = REPOSITORY.resolve("tests/fixtures/shards");

    @Test
    void fixtureValidatesCleanly() throws IOException {
        Validator.Report report = Validator.validate(FIXTURE, REPOSITORY);
        assertTrue(report.ok(), String.join("\n", report.errors()));
        assertEquals(1, report.bundles);
    }

    @Test
    void validatorReportsSchemaProblems(@TempDir Path directory) throws IOException {
        Path bundle = fixtureCopy(directory);
        ObjectNode relevance = JsonFiles.readObject(bundle.resolve("relevance.json"));
        relevance.put("class", "BOGUS");
        JsonFiles.write(bundle.resolve("relevance.json"), relevance);

        Validator.Report report = Validator.validate(directory.resolve("shards"), REPOSITORY);
        assertFalse(report.ok());
        assertTrue(report.errors().stream().anyMatch(error -> error.contains("enumeration")), String.join("\n", report.errors()));
    }

    @Test
    void validatorReportsCrossFileProblems(@TempDir Path directory) throws IOException {
        Path bundle = fixtureCopy(directory);
        ObjectNode metadata = JsonFiles.readObject(bundle.resolve("metadata.json"));
        metadata.put("paper_id", "someone-else");
        ObjectNode duplicate = metadata.path("sources").get(0).deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) metadata.path("sources")).add(duplicate);
        JsonFiles.write(bundle.resolve("metadata.json"), metadata);
        ObjectNode relevance = JsonFiles.readObject(bundle.resolve("relevance.json"));
        relevance.withArray("topics").add("not-a-topic");
        JsonFiles.write(bundle.resolve("relevance.json"), relevance);

        Validator.Report report = Validator.validate(directory.resolve("shards"), REPOSITORY);
        String errors = String.join("\n", report.errors());
        assertTrue(errors.contains("does not match the directory name"), errors);
        assertTrue(errors.contains("duplicate source ID within stage"), errors);
        assertTrue(errors.contains("unknown topics: [not-a-topic]"), errors);
    }

    /** Copies the fixture into the temp directory and returns its single bundle. */
    private static Path fixtureCopy(Path directory) throws IOException {
        Path copy = directory.resolve("shards");
        copyTree(FIXTURE, copy);
        try (Stream<Path> stream = Files.walk(copy)) {
            return stream.filter(path -> path.getFileName().toString().equals("metadata.json")).findFirst().orElseThrow().getParent();
        }
    }

    @Test
    void exportCarriesEveryFieldTheInterfaceReads() throws IOException {
        ObjectNode export = ShardExport.papersExport(FIXTURE);
        assertFalse(export.path("is_fixture_export").asBoolean());
        assertEquals(1, export.path("papers").size());
        JsonNode paper = export.path("papers").get(0);
        assertEquals("osdi", paper.path("venue").asText());
        assertEquals("Alice Fixture", paper.path("authors").get(0).asText());
        assertTrue(paper.has("abstract"));
        assertTrue(paper.has("track_or_session"));
        assertEquals("APPROVED", paper.path("summary").path("status").asText());
        assertTrue(paper.path("summary").path("background").isTextual());
        assertTrue(paper.path("summary").path("evaluation").path("results").isArray());
        assertEquals("APPROVED", paper.path("review").path("decision").asText());
        assertTrue(paper.path("links").has("official_page"));
        assertTrue(paper.path("institutions").isArray());
    }

    @Test
    void manifestFillsShardStatusesIntoTheBaseline() throws IOException {
        ObjectNode manifest = ShardExport.manifest(FIXTURE, REPOSITORY);
        assertEquals(48, manifest.path("venue_years").size());
        assertTrue(manifest.path("generated_at").asText().endsWith("Z"));
        JsonNode osdi2025 = null;
        for (JsonNode entry : manifest.path("venue_years")) {
            if (entry.path("venue").asText().equals("osdi") && entry.path("year").asInt() == 2025) {
                osdi2025 = entry;
            }
        }
        assertEquals("COMPLETE_PROCEEDINGS", osdi2025.path("collection_status").asText());
    }

    @Test
    void jsonWriterMatchesTheRepositoryFormatting() throws IOException {
        Path path;
        try (Stream<Path> stream = Files.walk(FIXTURE)) {
            path = stream.filter(candidate -> candidate.getFileName().toString().equals("metadata.json")).findFirst().orElseThrow();
        }
        assertEquals(Files.readString(path), JsonFiles.format(JsonFiles.read(path)));
        ObjectNode node = JsonFiles.mapper().createObjectNode();
        node.put("a", "é");
        node.putArray("empty");
        node.putObject("nested").putArray("list").add(1).add(2);
        assertEquals("{\n  \"a\": \"é\",\n  \"empty\": [],\n  \"nested\": {\n    \"list\": [\n      1,\n      2\n    ]\n  }\n}\n",
                JsonFiles.format(node));
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination);
                }
            }
        }
    }
}

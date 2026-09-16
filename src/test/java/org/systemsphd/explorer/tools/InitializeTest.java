package org.systemsphd.explorer.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitializeTest {
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();

    @Test
    void paperIdsMatchTheEstablishedScheme() {
        // Same title, venue, and year as a bundle created by the original tooling.
        assertEquals("osdi-2025-template-title-placeholder-de472f90",
                Initialize.paperId("osdi", 2025, "Template Title Placeholder"));
        assertEquals("cafe fast i o", Initialize.normalizeTitle("Café: Fast I/O!"));
        assertEquals("untitled", Initialize.slugify("!!!"));
        assertEquals(56, Initialize.slugify("word ".repeat(30)).length());
    }

    @Test
    void initPaperWritesAValidBundleAndVenueYear(@TempDir Path root) throws IOException {
        Path directory = Initialize.initializePaper(root, REPOSITORY, "nsdi", 2026, "A Brand New Paper", "agent-b");

        assertEquals(Initialize.paperId("nsdi", 2026, "A Brand New Paper"), directory.getFileName().toString());
        assertTrue(directory.getFileName().toString().startsWith("nsdi-2026-a-brand-new-paper-"));
        for (String filename : Shards.STAGE_FILES) {
            assertTrue(Files.exists(directory.resolve(filename)), filename);
        }
        ObjectNode metadata = JsonFiles.readObject(directory.resolve("metadata.json"));
        assertEquals("A Brand New Paper", metadata.path("title").asText());
        assertEquals(2026, metadata.path("year").asInt());
        assertEquals("agent-b", metadata.path("workflow").path("owner").asText());
        assertTrue(metadata.path("workflow").path("updated_at").asText().endsWith("Z"));
        assertEquals("NOT_STARTED", JsonFiles.readObject(directory.resolve("summary.json")).path("workflow").path("status").asText());
        assertTrue(Files.exists(root.resolve("nsdi/2026/venue-year.json")));

        Validator.Report report = Validator.validate(root, REPOSITORY);
        assertTrue(report.ok(), String.join("\n", report.errors()));

        assertThrows(IOException.class, () -> Initialize.initializePaper(root, REPOSITORY, "nsdi", 2026, "A Brand New Paper", "agent-b"));
        assertThrows(IOException.class, () -> Initialize.initializePaper(root, REPOSITORY, "isca", 2026, "Elsewhere", "agent-b"));
    }
}

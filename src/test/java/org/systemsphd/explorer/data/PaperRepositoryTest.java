package org.systemsphd.explorer.data;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.systemsphd.explorer.model.Paper;
import org.systemsphd.explorer.model.RepositorySnapshot;
import org.systemsphd.explorer.support.ExportBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperRepositoryTest {
    private static final Path FIXTURE = Path.of("site", "data", "fixture-papers.json");

    private final PaperRepository repository = new PaperRepository();

    @AfterEach
    void clearConfiguredPath() {
        System.clearProperty(PaperRepository.DATA_PROPERTY);
    }

    @Test
    void loadsTheSyntheticFixtureExport() throws IOException {
        RepositorySnapshot snapshot;
        try (InputStream stream = Files.newInputStream(FIXTURE)) {
            snapshot = repository.load(stream, "fixture");
        }

        assertTrue(snapshot.isFixture());
        assertEquals("fixture", snapshot.getSourceLabel());
        assertEquals(8, snapshot.getPapers().size());

        Paper kernel = snapshot.getPapers().get(0);
        assertEquals("fixture-os-kernel", kernel.getId());
        assertEquals("osdi", kernel.getVenue());
        assertEquals(2025, kernel.getYear());
        assertEquals(List.of("Avery Example", "Morgan Sample"), kernel.getAuthors());
        // Fixture faculty and groups are objects with names, not ID lists.
        assertTrue(kernel.getSearchText().contains("rowan placeholder"));
        assertTrue(kernel.getSearchText().contains("safe systems fixture lab"));
    }

    @Test
    void loadsTheGeneratedExportShape() throws IOException {
        byte[] export = new ExportBuilder()
                .paper("osdi-2025-abc", "osdi", 2025)
                .title("Real Title")
                .authors("A. Author", "B. Author")
                .facultyIds("faculty-a")
                .groupIds("group-b")
                .institutions("Example University")
                .add()
                .toBytes();

        RepositorySnapshot snapshot = repository.load(new ByteArrayInputStream(export), "generated");

        assertFalse(snapshot.isFixture());
        assertEquals(1, snapshot.getPapers().size());
        Paper paper = snapshot.getPapers().get(0);
        assertEquals("Real Title", paper.getTitle());
        assertTrue(paper.getSearchText().contains("faculty-a"));
        assertTrue(paper.getSearchText().contains("group-b"));
        assertTrue(paper.getSearchText().contains("example university"));
    }

    @Test
    void rejectsExportsWithoutAPapersArray() {
        InputStream stream = new ByteArrayInputStream("{\"papers\": {}}".getBytes(StandardCharsets.UTF_8));
        IOException error = assertThrows(IOException.class, () -> repository.load(stream, "bad"));
        assertTrue(error.getMessage().contains("papers array"));
    }

    @Test
    void rejectsRecordsMissingIdentity() {
        byte[] export = new ExportBuilder()
                .paper("osdi-2025-abc", "osdi", 2025).without("title").add()
                .toBytes();
        assertThrows(IOException.class, () -> repository.load(new ByteArrayInputStream(export), "bad"));
    }

    @Test
    void prefersAnExplicitlyConfiguredFile(@TempDir Path directory) throws IOException {
        Path configured = directory.resolve("papers.json");
        Files.write(configured, new ExportBuilder()
                .paper("configured", "nsdi", 2024).add()
                .toBytes());
        System.setProperty(PaperRepository.DATA_PROPERTY, configured.toString());

        RepositorySnapshot snapshot = repository.load(context(Map.of(
                "/WEB-INF/data/papers.json", packaged("packaged"),
                "/WEB-INF/data/fixture-papers.json", packaged("fixture")
        )));

        assertEquals("configured", snapshot.getPapers().get(0).getId());
        assertEquals(configured.toAbsolutePath().normalize().toString(), snapshot.getSourceLabel());
    }

    @Test
    void prefersThePackagedGeneratedExportOverTheFixture() throws IOException {
        RepositorySnapshot snapshot = repository.load(context(Map.of(
                "/WEB-INF/data/papers.json", packaged("packaged"),
                "/WEB-INF/data/fixture-papers.json", packaged("fixture")
        )));
        assertEquals("packaged", snapshot.getPapers().get(0).getId());
        assertEquals("generated paper export", snapshot.getSourceLabel());
    }

    @Test
    void fallsBackToThePackagedFixture() throws IOException {
        RepositorySnapshot snapshot = repository.load(context(Map.of(
                "/WEB-INF/data/fixture-papers.json", packaged("fixture")
        )));
        assertEquals("fixture", snapshot.getPapers().get(0).getId());
        assertEquals("synthetic fixture export", snapshot.getSourceLabel());
    }

    @Test
    void failsWhenNothingIsPackaged() {
        assertThrows(IOException.class, () -> repository.load(context(Map.of())));
    }

    private static byte[] packaged(String id) {
        return new ExportBuilder().paper(id, "osdi", 2025).add().toBytes();
    }

    /** A ServletContext that only knows how to answer getResourceAsStream. */
    private static ServletContext context(Map<String, byte[]> resources) {
        return (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[] {ServletContext.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getResourceAsStream")) {
                        byte[] content = resources.get((String) arguments[0]);
                        return content == null ? null : new ByteArrayInputStream(content);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}

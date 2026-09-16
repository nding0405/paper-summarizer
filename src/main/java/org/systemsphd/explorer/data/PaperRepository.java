package org.systemsphd.explorer.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import org.systemsphd.explorer.catalog.Taxonomy;
import org.systemsphd.explorer.model.Paper;
import org.systemsphd.explorer.model.RepositorySnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PaperRepository {
    public static final String DATA_PROPERTY = "systems.phd.papers";
    public static final String DATA_ENVIRONMENT_VARIABLE = "SYSTEMS_PHD_PAPERS";

    private final ObjectMapper objectMapper;
    private final Taxonomy topicLabels;

    public PaperRepository() {
        this(Taxonomy.empty());
    }

    public PaperRepository(Taxonomy topicLabels) {
        this.objectMapper = new ObjectMapper();
        this.topicLabels = topicLabels;
    }

    public RepositorySnapshot load(ServletContext context) throws IOException {
        String configuredPath = System.getProperty(DATA_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv(DATA_ENVIRONMENT_VARIABLE);
        }
        if (configuredPath != null && !configuredPath.isBlank()) {
            Path path = Path.of(configuredPath).toAbsolutePath().normalize();
            try (InputStream stream = Files.newInputStream(path)) {
                return load(stream, path.toString());
            }
        }

        try (InputStream generated = context.getResourceAsStream("/WEB-INF/data/papers.json")) {
            if (generated != null) {
                return load(generated, "generated paper export");
            }
        }
        try (InputStream fixture = context.getResourceAsStream("/WEB-INF/data/fixture-papers.json")) {
            if (fixture != null) {
                return load(fixture, "synthetic fixture export");
            }
        }
        throw new IOException("No paper export was packaged with the application.");
    }

    public RepositorySnapshot load(InputStream stream, String sourceLabel) throws IOException {
        JsonNode payload = objectMapper.readTree(stream);
        JsonNode records = payload.path("papers");
        if (!records.isArray()) {
            throw new IOException("The paper export must contain a papers array.");
        }
        List<Paper> papers = new ArrayList<>(records.size());
        for (JsonNode record : records) {
            Paper paper = Paper.from(record, topicLabels);
            if (paper.getId().isBlank() || paper.getTitle().isBlank() || paper.getVenue().isBlank()) {
                throw new IOException("The paper export contains a record without an id, title, or venue.");
            }
            papers.add(paper);
        }
        return new RepositorySnapshot(
                papers,
                payload.path("is_fixture_export").asBoolean(false),
                sourceLabel
        );
    }
}

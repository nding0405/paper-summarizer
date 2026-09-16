package org.systemsphd.explorer.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.systemsphd.explorer.catalog.ConferenceCatalog;
import org.systemsphd.explorer.data.PaperRepository;
import org.systemsphd.explorer.model.RepositorySnapshot;
import org.systemsphd.explorer.web.ExplorerService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds small paper exports in the shape produced by {@code make site-data}
 * so tests do not depend on the real corpus.
 */
public final class ExportBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ArrayNode papers = MAPPER.createArrayNode();
    private boolean fixture;

    public ExportBuilder fixture(boolean value) {
        fixture = value;
        return this;
    }

    public PaperBuilder paper(String id, String venue, int year) {
        return new PaperBuilder(id, venue, year);
    }

    public String toJson() {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("schema_version", "1.0.0");
        payload.put("is_fixture_export", fixture);
        payload.set("papers", papers);
        return payload.toPrettyString();
    }

    public byte[] toBytes() {
        return toJson().getBytes(StandardCharsets.UTF_8);
    }

    public RepositorySnapshot snapshot() {
        try {
            return new PaperRepository().load(new ByteArrayInputStream(toBytes()), "test export");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public ExplorerService service() {
        return new ExplorerService(snapshot(), new ConferenceCatalog());
    }

    public final class PaperBuilder {
        private final ObjectNode node = MAPPER.createObjectNode();
        private final ObjectNode links = MAPPER.createObjectNode();

        private PaperBuilder(String id, String venue, int year) {
            node.put("id", id);
            node.put("title", "Paper " + id);
            node.put("venue", venue);
            node.put("year", year);
            node.put("takeaway", "");
            node.put("relevance_class", "CORE_SYSTEMS");
            node.put("has_code", false);
            node.put("has_artifact", false);
            for (String field : List.of("authors", "topics", "system_layers", "methods",
                    "institutions", "locations", "faculty_ids", "group_ids")) {
                node.putArray(field);
            }
            for (String field : List.of("official_page", "official_pdf", "code", "artifact")) {
                links.putNull(field);
            }
            node.set("links", links);
        }

        public PaperBuilder title(String title) {
            node.put("title", title);
            return this;
        }

        public PaperBuilder track(String track) {
            node.put("track_or_session", track);
            return this;
        }

        public PaperBuilder abstractText(String text) {
            node.put("abstract", text);
            return this;
        }

        /** Attaches a raw summary object in the export shape. */
        public PaperBuilder summary(ObjectNode summary) {
            node.set("summary", summary);
            return this;
        }

        public PaperBuilder review(ObjectNode review) {
            node.set("review", review);
            return this;
        }

        public PaperBuilder authors(String... authors) {
            return list("authors", authors);
        }

        public PaperBuilder takeaway(String takeaway) {
            node.put("takeaway", takeaway);
            return this;
        }

        public PaperBuilder relevance(String relevanceClass) {
            node.put("relevance_class", relevanceClass);
            return this;
        }

        public PaperBuilder topics(String... topics) {
            return list("topics", topics);
        }

        public PaperBuilder methods(String... methods) {
            return list("methods", methods);
        }

        public PaperBuilder layers(String... layers) {
            return list("system_layers", layers);
        }

        public PaperBuilder institutions(String... institutions) {
            return list("institutions", institutions);
        }

        public PaperBuilder locations(String... locations) {
            return list("locations", locations);
        }

        public PaperBuilder facultyIds(String... ids) {
            return list("faculty_ids", ids);
        }

        public PaperBuilder groupIds(String... ids) {
            return list("group_ids", ids);
        }

        public PaperBuilder code(String url) {
            node.put("has_code", url != null);
            links.put("code", url);
            return this;
        }

        public PaperBuilder artifact(String url) {
            node.put("has_artifact", url != null);
            links.put("artifact", url);
            return this;
        }

        public PaperBuilder officialPage(String url) {
            links.put("official_page", url);
            return this;
        }

        public PaperBuilder officialPdf(String url) {
            links.put("official_pdf", url);
            return this;
        }

        /** Removes a field entirely, for testing malformed records. */
        public PaperBuilder without(String field) {
            node.remove(field);
            return this;
        }

        public ExportBuilder add() {
            papers.add(node);
            return ExportBuilder.this;
        }

        private PaperBuilder list(String field, String... values) {
            ArrayNode array = node.putArray(field);
            for (String value : values) {
                array.add(value);
            }
            return this;
        }
    }
}

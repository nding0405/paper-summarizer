package org.systemsphd.explorer.tools;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes the repository's JSON files. Output matches the layout the
 * data files already use (two-space indent, one array element per line,
 * {@code "key": value}, empty containers as {@code []} and {@code {}}, UTF-8
 * without escaping, trailing newline) so rewrites do not churn diffs.
 */
public final class JsonFiles {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DefaultPrettyPrinter PRETTY = new DefaultPrettyPrinter()
            .withSeparators(Separators.createDefaultInstance()
                    .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                    .withObjectEmptySeparator("")
                    .withArrayEmptySeparator(""))
            .withObjectIndenter(new DefaultIndenter("  ", "\n"))
            .withArrayIndenter(new DefaultIndenter("  ", "\n"));

    private JsonFiles() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static JsonNode read(Path path) throws IOException {
        try {
            return MAPPER.readTree(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new IOException(path + ": " + exception.getMessage(), exception);
        }
    }

    public static ObjectNode readObject(Path path) throws IOException {
        JsonNode node = read(path);
        if (!(node instanceof ObjectNode object)) {
            throw new IOException(path + ": expected a JSON object");
        }
        return object;
    }

    public static String format(JsonNode node) throws IOException {
        return MAPPER.writer(PRETTY.createInstance()).writeValueAsString(node) + "\n";
    }

    /** Writes atomically: to a temporary sibling first, then a rename over the target. */
    public static void write(Path path, JsonNode node) throws IOException {
        Path temporary = path.resolveSibling("." + path.getFileName() + ".tmp");
        Files.writeString(temporary, format(node), StandardCharsets.UTF_8);
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    static {
        MAPPER.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }
}

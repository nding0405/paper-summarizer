package org.systemsphd.explorer.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates new paper bundles and venue-year records from the stage templates
 * in {@code src/main/resources/templates}, so every agent starts from the same
 * skeleton and the same deterministic paper ID.
 */
public final class Initialize {
    private static final Set<Integer> SUPPORTED_YEARS = Set.of(2023, 2024, 2025, 2026);
    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");
    private static final int SLUG_LENGTH = 56;

    private Initialize() {
    }

    // ------------------------------------------------------------------
    // IDs
    // ------------------------------------------------------------------

    /** Lower-case ASCII words only, e.g. "Café: Fast I/O!" becomes "cafe fast i o". */
    static String normalizeTitle(String title) {
        String ascii = Normalizer.normalize(title, Normalizer.Form.NFKD).replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT);
        StringBuilder words = new StringBuilder();
        Matcher matcher = WORD.matcher(ascii);
        while (matcher.find()) {
            if (words.length() > 0) {
                words.append(' ');
            }
            words.append(matcher.group());
        }
        return words.toString();
    }

    static String slugify(String value) {
        String slug = normalizeTitle(value).replace(' ', '-');
        if (slug.length() > SLUG_LENGTH) {
            slug = slug.substring(0, SLUG_LENGTH);
        }
        slug = slug.replaceAll("-+$", "");
        return slug.isEmpty() ? "untitled" : slug;
    }

    /** {@code <venue>-<year>-<title-slug>-<8 hex of sha256(normalized title)>}. */
    public static String paperId(String venue, int year, String title) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizeTitle(title).getBytes(StandardCharsets.UTF_8));
            return venue + "-" + year + "-" + slugify(title) + "-" + HexFormat.of().formatHex(digest).substring(0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    // ------------------------------------------------------------------
    // Commands
    // ------------------------------------------------------------------

    static int runInitPaper(List<String> arguments) throws IOException {
        Main.Options options = new Main.Options(arguments);
        Path repository = Shards.repositoryRoot(options.get("repo", null));
        Path root = Path.of(options.get("root", repository.resolve("data/shards").toString())).toAbsolutePath().normalize();
        Path directory = initializePaper(root, repository, options.require("venue"),
                Integer.parseInt(options.require("year")), options.require("title"), options.require("owner"));
        System.out.println(directory);
        return 0;
    }

    static int runInitVenue(List<String> arguments) throws IOException {
        Main.Options options = new Main.Options(arguments);
        Path repository = Shards.repositoryRoot(options.get("repo", null));
        Path root = Path.of(options.get("root", repository.resolve("data/shards").toString())).toAbsolutePath().normalize();
        Path file = initializeVenueYear(root, repository, options.require("venue"),
                Integer.parseInt(options.require("year")), options.require("owner"));
        System.out.println(file);
        return 0;
    }

    public static Path initializeVenueYear(Path root, Path repository, String venue, int year, String owner) throws IOException {
        checkVenueYear(repository, venue, year);
        Path destination = root.resolve(venue).resolve(String.valueOf(year)).resolve("venue-year.json");
        if (Files.exists(destination)) {
            throw new IOException("venue-year record already exists: " + destination);
        }
        Files.createDirectories(destination.getParent());
        JsonFiles.write(destination, fill(template("venue-year.json"), Map.of(
                "venue", venue, "year", String.valueOf(year), "owner", owner.trim(), "now", now())));
        return destination;
    }

    public static Path initializePaper(Path root, Path repository, String venue, int year, String title, String owner)
            throws IOException {
        checkVenueYear(repository, venue, year);
        if (title.isBlank()) {
            throw new IOException("Paper title must not be empty");
        }
        if (owner.isBlank()) {
            throw new IOException("Owner must not be empty");
        }
        Path shard = root.resolve(venue).resolve(String.valueOf(year));
        if (!Files.exists(shard.resolve("venue-year.json"))) {
            initializeVenueYear(root, repository, venue, year, owner);
        }
        String paperId = paperId(venue, year, title.trim());
        Path directory = shard.resolve("papers").resolve(paperId);
        if (Files.exists(directory)) {
            throw new IOException("Paper bundle already exists: " + directory);
        }
        Files.createDirectories(directory);
        Map<String, String> values = Map.of("paper_id", paperId, "title", title.trim(), "venue", venue,
                "year", String.valueOf(year), "owner", owner.trim(), "now", now());
        for (String filename : Shards.STAGE_FILES) {
            JsonFiles.write(directory.resolve(filename), fill(template(filename), values));
        }
        return directory;
    }

    private static void checkVenueYear(Path repository, String venue, int year) throws IOException {
        Set<String> venues = new HashSet<>();
        JsonFiles.readObject(repository.resolve("config/venues.json")).path("venues")
                .forEach(item -> venues.add(item.path("slug").asText()));
        if (!venues.contains(venue)) {
            throw new IOException("Unsupported venue '" + venue + "'; choose one of " + new java.util.TreeSet<>(venues));
        }
        if (!SUPPORTED_YEARS.contains(year)) {
            throw new IOException("Unsupported year " + year + "; choose one of " + new java.util.TreeSet<>(SUPPORTED_YEARS));
        }
    }

    private static ObjectNode template(String filename) throws IOException {
        try (InputStream stream = Initialize.class.getResourceAsStream("/templates/" + filename)) {
            if (stream == null) {
                throw new IOException("Missing template " + filename);
            }
            return (ObjectNode) JsonFiles.mapper().readTree(stream);
        }
    }

    /** Replaces {@code {{name}}} placeholders; {@code year} becomes a number. */
    static JsonNode fill(JsonNode node, Map<String, String> values) {
        if (node instanceof ObjectNode object) {
            ObjectNode copy = JsonFiles.mapper().createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                copy.set(entry.getKey(), fill(entry.getValue(), values));
            }
            return copy;
        }
        if (node instanceof ArrayNode array) {
            ArrayNode copy = JsonFiles.mapper().createArrayNode();
            array.forEach(item -> copy.add(fill(item, values)));
            return copy;
        }
        if (node instanceof TextNode text && text.asText().startsWith("{{") && text.asText().endsWith("}}")) {
            String name = text.asText().substring(2, text.asText().length() - 2);
            String value = values.get(name);
            if (value == null) {
                return node;
            }
            return name.equals("year") ? JsonFiles.mapper().getNodeFactory().numberNode(Integer.parseInt(value))
                    : JsonFiles.mapper().getNodeFactory().textNode(value);
        }
        return node;
    }

    private static String now() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }
}

package org.systemsphd.explorer.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects published abstracts into each paper's {@code metadata.json}.
 *
 * <p>The run is a sequence of passes over the papers that still lack an
 * abstract, fastest and most authoritative sources first:
 * <ol>
 *   <li>official paper pages that publish the abstract in HTML (USENIX, NDSS);</li>
 *   <li>the OpenAlex API, by DOI and then by title search;</li>
 *   <li>the Semantic Scholar batch endpoint, by DOI (a hundred papers per request);</li>
 *   <li>Semantic Scholar exact-title matching, for papers without a DOI;</li>
 *   <li>the arXiv API, by exact title, for papers with a preprint.</li>
 * </ol>
 * ACM and IEEE pages are never scraped; their abstracts come from the APIs.
 * Every stored abstract carries a {@code sources} record so the text can be
 * traced to where it was read. Papers that already have an abstract are
 * skipped, so an interrupted run is resumable.
 */
public final class AbstractCollector {
    static final String USER_AGENT = "systems-phd-explorer/1.0 (abstract collector)";
    static final int MIN_ABSTRACT_LENGTH = 80;
    static final double TITLE_MATCH_RATIO = 0.92;
    static final int SEMANTIC_SCHOLAR_BATCH_SIZE = 100;
    static final List<String> ALL_SOURCES = List.of("official", "openalex", "s2-batch", "s2-match", "arxiv");

    private static final String SEMANTIC_SCHOLAR_API = "https://api.semanticscholar.org/graph/v1";
    private static final String OPENALEX_API = "https://api.openalex.org";
    private static final String ARXIV_API = "http://export.arxiv.org/api/query";

    /** Minimum seconds between requests to the same host. */
    private static final Map<String, Double> LANE_INTERVALS = Map.of(
            "usenix", 0.6, "ndss", 0.6, "semantic-scholar", 5.0, "openalex", 0.25, "arxiv", 3.0
    );

    private static final Pattern USENIX_ABSTRACT = Pattern.compile(
            "field-name-field-paper-description.*?<div class=\"field-item[^\"]*\">(.*?)</div>\\s*</div>", Pattern.DOTALL);
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("(?i)</p\\s*>|<br\\s*/?>|</jats:p>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern LEADING_LABEL = Pattern.compile("(?i)^abstract[:.\\s—-]*");
    private static final Pattern PLACEHOLDER = Pattern.compile("(?i)^(no abstract|abstract (is )?(not )?available|n/a)\\b.*");
    private static final Pattern DOI_PREFIX = Pattern.compile("(?i)^(https?://(dx\\.)?doi\\.org/|doi:\\s*)");

    private AbstractCollector() {
    }

    // ------------------------------------------------------------------
    // Pure helpers
    // ------------------------------------------------------------------

    /** Strips HTML/JATS tags, unescapes entities, and normalizes whitespace, keeping paragraph breaks. */
    static String cleanText(String fragment) {
        String text = PARAGRAPH_BREAK.matcher(fragment).replaceAll("\n\n");
        text = TAG.matcher(text).replaceAll(" ");
        text = Entities.unescape(text).replace(' ', ' ');
        List<String> paragraphs = new ArrayList<>();
        for (String paragraph : text.split("\n\n")) {
            String collapsed = paragraph.replaceAll("\\s+", " ").trim();
            if (!collapsed.isEmpty()) {
                paragraphs.add(collapsed);
            }
        }
        text = String.join("\n\n", paragraphs);
        return LEADING_LABEL.matcher(text).replaceFirst("").trim();
    }

    /** The cleaned abstract, or null when it is missing, too short, or a placeholder. */
    static String usableAbstract(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = cleanText(text.contains("<") ? text : Entities.escape(text));
        if (cleaned.length() < MIN_ABSTRACT_LENGTH || PLACEHOLDER.matcher(cleaned).matches()) {
            return null;
        }
        return cleaned;
    }

    /** USENIX keeps the abstract in the {@code field-name-field-paper-description} block. */
    static String extractUsenix(String page) {
        Matcher matcher = USENIX_ABSTRACT.matcher(page);
        return matcher.find() ? usableAbstract(matcher.group(1)) : null;
    }

    /**
     * NDSS keeps the abstract in {@code div.paper-data} after a {@code <strong>}
     * author block and before the {@code paper-buttons} block; it may span
     * several paragraphs, all of which are kept.
     */
    static String extractNdss(String page) {
        int marker = page.indexOf("class=\"paper-data\"");
        if (marker < 0) {
            return null;
        }
        int start = page.indexOf('>', marker) + 1;
        int end = page.indexOf("class=\"paper-buttons\"", start);
        if (end < 0) {
            end = page.indexOf("</article>", start);
        }
        if (end > 0) {
            int tagStart = page.lastIndexOf('<', end);
            end = tagStart > start ? tagStart : end;
        } else {
            end = Math.min(page.length(), start + 60000);
        }
        String section = page.substring(start, end).replaceAll("(?s)<strong>.*?</strong>", " ");
        return usableAbstract(section);
    }

    /** Rebuilds the abstract text from OpenAlex's inverted index. */
    static String openAlexAbstract(JsonNode invertedIndex) {
        if (invertedIndex == null || !invertedIndex.isObject() || invertedIndex.isEmpty()) {
            return null;
        }
        TreeMap<Integer, String> positions = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = invertedIndex.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            for (JsonNode index : entry.getValue()) {
                positions.put(index.asInt(), entry.getKey());
            }
        }
        return usableAbstract(Entities.escape(String.join(" ", positions.values())));
    }

    static String normalizeTitle(String title) {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    static boolean titlesMatch(String expected, String candidate) {
        if (candidate == null) {
            return false;
        }
        String left = normalizeTitle(expected);
        String right = normalizeTitle(candidate);
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        return left.equals(right) || similarity(left, right) >= TITLE_MATCH_RATIO;
    }

    /** Ratio in the style of difflib: 2 * matching characters / total length, via LCS. */
    static double similarity(String left, String right) {
        int[][] table = new int[left.length() + 1][right.length() + 1];
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                table[i][j] = left.charAt(i - 1) == right.charAt(j - 1)
                        ? table[i - 1][j - 1] + 1
                        : Math.max(table[i - 1][j], table[i][j - 1]);
            }
        }
        return 2.0 * table[left.length()][right.length()] / (left.length() + right.length());
    }

    static String normalizeDoi(String value) {
        if (value == null) {
            return null;
        }
        String doi = DOI_PREFIX.matcher(value.trim()).replaceFirst("");
        return doi.isEmpty() ? null : doi;
    }

    /** Which throttled lane fetches this paper: an official page or the APIs. */
    static String laneFor(JsonNode metadata) {
        String page = text(metadata.path("links"), "official_page");
        String host;
        try {
            host = page.isEmpty() ? "" : URI.create(page).getHost();
        } catch (IllegalArgumentException exception) {
            host = "";
        }
        host = host == null ? "" : host.toLowerCase(Locale.ROOT);
        if (host.endsWith("usenix.org")) {
            return "usenix";
        }
        if (host.endsWith("ndss-symposium.org")) {
            return "ndss";
        }
        return "api";
    }

    /** Source IDs are unique across the corpus, so they carry the venue-year and the paper hash. */
    static String uniqueSourceId(JsonNode metadata) {
        String paperId = text(metadata, "paper_id");
        String base = "src-abstract-" + text(metadata, "venue") + "-" + metadata.path("year").asText()
                + "-" + paperId.substring(paperId.lastIndexOf('-') + 1);
        // A previously stored abstract source is about to be replaced, so its ID is free.
        String replaced = text(metadata, "abstract_source_id");
        Set<String> existing = new LinkedHashSet<>();
        for (JsonNode source : metadata.path("sources")) {
            if (!text(source, "id").equals(replaced)) {
                existing.add(text(source, "id"));
            }
        }
        String candidate = base;
        for (int suffix = 2; existing.contains(candidate); suffix++) {
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    /**
     * A copy of the metadata with the abstract inserted after the title and the
     * source appended. Any earlier abstract source is dropped first.
     */
    static ObjectNode withAbstract(ObjectNode metadata, String abstractText, ObjectNode source) {
        ObjectNode updated = JsonFiles.mapper().createObjectNode();
        String previousSource = text(metadata, "abstract_source_id");
        Iterator<Map.Entry<String, JsonNode>> fields = metadata.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (entry.getKey().equals("abstract") || entry.getKey().equals("abstract_source_id")) {
                continue;
            }
            updated.set(entry.getKey(), entry.getValue());
            if (entry.getKey().equals("title")) {
                updated.put("abstract", abstractText);
                updated.put("abstract_source_id", source.path("id").asText());
            }
        }
        ArrayNode sources = updated.putArray("sources");
        for (JsonNode existing : metadata.path("sources")) {
            String id = text(existing, "id");
            if (!id.equals(source.path("id").asText()) && !(id.equals(previousSource) && !previousSource.isEmpty())) {
                sources.add(existing);
            }
        }
        sources.add(source);
        return updated;
    }

    static ObjectNode sourceRecord(String id, String url, String kind, String title, String publisher,
                                   boolean official, byte[] content) {
        ObjectNode source = JsonFiles.mapper().createObjectNode();
        source.put("id", id);
        source.put("url", url);
        source.put("kind", kind);
        if (title == null) {
            source.putNull("title");
        } else {
            source.put("title", title);
        }
        source.put("publisher", publisher);
        source.put("retrieved_at", now());
        source.put("content_hash", "sha256:" + sha256(content));
        source.put("official", official);
        source.putArray("supports").add("Published abstract text.");
        return source;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String now() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

    static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText().trim() : "";
    }

    // ------------------------------------------------------------------
    // HTTP
    // ------------------------------------------------------------------

    static final class FetchException extends IOException {
        FetchException(String message) {
            super(message);
        }
    }

    record Response(int status, byte[] body, String url) {
        JsonNode json() throws IOException {
            return JsonFiles.mapper().readTree(body);
        }

        String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    /** HttpClient wrapper with a user agent, per-lane throttling, and retries. */
    static final class Fetcher {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build();
        private final String mailto;
        private final String s2ApiKey;
        private final Consumer<String> log;
        private final Map<String, Long> lastRequest = new HashMap<>();
        private final Map<String, Object> locks = new HashMap<>();

        Fetcher(String mailto, String s2ApiKey, Consumer<String> log) {
            this.mailto = mailto;
            this.s2ApiKey = s2ApiKey;
            this.log = log;
        }

        String mailto() {
            return mailto;
        }

        private void throttle(String lane) throws InterruptedException {
            Object lock;
            synchronized (locks) {
                lock = locks.computeIfAbsent(lane, key -> new Object());
            }
            synchronized (lock) {
                long interval = (long) (LANE_INTERVALS.getOrDefault(lane, 1.0) * 1000);
                long wait = interval - (System.currentTimeMillis() - lastRequest.getOrDefault(lane, 0L));
                if (wait > 0) {
                    Thread.sleep(wait);
                }
                lastRequest.put(lane, System.currentTimeMillis());
            }
        }

        Response get(String url, String lane, String accept) throws IOException, InterruptedException {
            return send(url, lane, accept, null, 3, 2.0);
        }

        /** A 404 is returned; 429s and server errors retry with back-off, then raise. */
        Response send(String url, String lane, String accept, byte[] payload, int retries, double delaySeconds)
                throws IOException, InterruptedException {
            double delay = delaySeconds;
            for (int attempt = 0; attempt <= retries; attempt++) {
                throttle(lane);
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", USER_AGENT + (mailto == null ? "" : " (mailto:" + mailto + ")"))
                        .header("Accept", accept);
                if ("semantic-scholar".equals(lane) && s2ApiKey != null) {
                    builder.header("x-api-key", s2ApiKey);
                }
                if (payload != null) {
                    builder.header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(payload));
                }
                HttpResponse<byte[]> response;
                try {
                    response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
                } catch (IOException exception) {
                    if (attempt < retries) {
                        log.accept(String.format("    %s: %s, retrying in %.0fs (%d/%d)", lane, exception.getMessage(), delay, attempt + 1, retries));
                        Thread.sleep((long) (delay * 1000));
                        delay *= 2;
                        continue;
                    }
                    throw new FetchException(exception.getMessage() + " for " + url);
                }
                int status = response.statusCode();
                if (status == 404) {
                    return new Response(404, new byte[0], url);
                }
                if (status == 429 || status >= 500) {
                    if (attempt < retries) {
                        double pause = response.headers().firstValue("Retry-After")
                                .filter(value -> value.matches("\\d+")).map(Double::parseDouble).orElse(delay);
                        pause = Math.min(pause, 60.0);
                        log.accept(String.format("    %s: HTTP %d, retrying in %.0fs (%d/%d)", lane, status, pause, attempt + 1, retries));
                        Thread.sleep((long) (pause * 1000));
                        delay *= 2;
                        continue;
                    }
                    throw new FetchException("HTTP " + status + " for " + url);
                }
                if (status != 200) {
                    throw new FetchException("HTTP " + status + " for " + url);
                }
                return new Response(status, response.body(), url);
            }
            throw new FetchException("gave up on " + url);
        }
    }

    // ------------------------------------------------------------------
    // Strategies
    // ------------------------------------------------------------------

    record Found(String text, ObjectNode source) {
    }

    interface Strategy {
        Found find(ObjectNode metadata, Fetcher fetcher) throws IOException, InterruptedException;
    }

    static Strategy officialPage(String lane) {
        return switch (lane) {
            case "usenix" -> (metadata, fetcher) -> officialPage(metadata, fetcher, lane, "USENIX", AbstractCollector::extractUsenix);
            case "ndss" -> (metadata, fetcher) -> officialPage(metadata, fetcher, lane, "NDSS", AbstractCollector::extractNdss);
            default -> null;
        };
    }

    private static Found officialPage(ObjectNode metadata, Fetcher fetcher, String lane, String publisher,
                                      java.util.function.Function<String, String> extract)
            throws IOException, InterruptedException {
        String url = text(metadata.path("links"), "official_page");
        Response response = fetcher.get(url, lane, "text/html");
        if (response.status() != 200) {
            return null;
        }
        String abstractText = extract.apply(response.text());
        if (abstractText == null) {
            return null;
        }
        return new Found(abstractText, sourceRecord(uniqueSourceId(metadata), url, "paper-page",
                text(metadata, "title"), publisher, true, response.body()));
    }

    static Found openAlex(ObjectNode metadata, Fetcher fetcher) throws IOException, InterruptedException {
        String doi = normalizeDoi(text(metadata.path("links"), "doi"));
        String select = "id,title,abstract_inverted_index";
        String mailto = fetcher.mailto() == null ? "" : "&mailto=" + encode(fetcher.mailto());
        List<Map.Entry<String, Boolean>> attempts = new ArrayList<>();
        if (doi != null) {
            attempts.add(Map.entry(OPENALEX_API + "/works/doi:" + encodePath(doi) + "?select=" + select + mailto, false));
        }
        attempts.add(Map.entry(OPENALEX_API + "/works?filter=title.search:" + encode(text(metadata, "title"))
                + "&select=" + select + "&per-page=3" + mailto, true));
        for (Map.Entry<String, Boolean> attempt : attempts) {
            Response response = fetcher.get(attempt.getKey(), "openalex", "application/json");
            if (response.status() != 200) {
                continue;
            }
            JsonNode payload = response.json();
            Iterable<JsonNode> records = attempt.getValue() ? payload.path("results") : List.of(payload);
            for (JsonNode record : records) {
                if (attempt.getValue() && !titlesMatch(text(metadata, "title"), text(record, "title"))) {
                    continue;
                }
                String abstractText = openAlexAbstract(record.path("abstract_inverted_index"));
                if (abstractText == null) {
                    continue;
                }
                return new Found(abstractText, sourceRecord(uniqueSourceId(metadata), attempt.getKey(), "openalex",
                        text(record, "title"), "OpenAlex", false, response.body()));
            }
        }
        return null;
    }

    static Found semanticScholarMatch(ObjectNode metadata, Fetcher fetcher) throws IOException, InterruptedException {
        String url = SEMANTIC_SCHOLAR_API + "/paper/search/match?query=" + encode(text(metadata, "title")) + "&fields=title,abstract";
        Response response = fetcher.send(url, "semantic-scholar", "application/json", null, 4, 15.0);
        if (response.status() != 200) {
            return null;
        }
        for (JsonNode record : response.json().path("data")) {
            if (!titlesMatch(text(metadata, "title"), text(record, "title"))) {
                continue;
            }
            String abstractText = usableAbstract(record.path("abstract").isTextual() ? record.path("abstract").asText() : null);
            if (abstractText == null) {
                continue;
            }
            return new Found(abstractText, sourceRecord(uniqueSourceId(metadata), url, "semantic-scholar",
                    text(record, "title"), "Semantic Scholar", false, response.body()));
        }
        return null;
    }

    /** Looks up many DOIs in one request; returns findings keyed by paper ID. */
    static Map<String, Found> semanticScholarBatch(List<ObjectNode> batch, Fetcher fetcher)
            throws IOException, InterruptedException {
        Map<String, String> dois = new LinkedHashMap<>();
        for (ObjectNode metadata : batch) {
            String doi = normalizeDoi(text(metadata.path("links"), "doi"));
            if (doi != null) {
                dois.put(text(metadata, "paper_id"), doi);
            }
        }
        if (dois.isEmpty()) {
            return Map.of();
        }
        String url = SEMANTIC_SCHOLAR_API + "/paper/batch?fields=title,abstract,externalIds";
        ObjectNode payload = JsonFiles.mapper().createObjectNode();
        ArrayNode ids = payload.putArray("ids");
        dois.values().forEach(doi -> ids.add("DOI:" + doi));
        Response response = fetcher.send(url, "semantic-scholar", "application/json",
                JsonFiles.mapper().writeValueAsBytes(payload), 4, 15.0);
        if (response.status() != 200) {
            return Map.of();
        }
        Map<String, JsonNode> byDoi = new HashMap<>();
        for (JsonNode record : response.json()) {
            String doi = normalizeDoi(text(record.path("externalIds"), "DOI"));
            if (doi != null) {
                byDoi.put(doi.toLowerCase(Locale.ROOT), record);
            }
        }
        Map<String, Found> found = new LinkedHashMap<>();
        for (ObjectNode metadata : batch) {
            String doi = dois.get(text(metadata, "paper_id"));
            JsonNode record = doi == null ? null : byDoi.get(doi.toLowerCase(Locale.ROOT));
            String abstractText = record == null || !record.path("abstract").isTextual()
                    ? null : usableAbstract(record.path("abstract").asText());
            if (abstractText == null) {
                continue;
            }
            found.put(text(metadata, "paper_id"), new Found(abstractText, sourceRecord(uniqueSourceId(metadata), url,
                    "semantic-scholar", text(record, "title"), "Semantic Scholar", false, response.body())));
        }
        return found;
    }

    static Found arxiv(ObjectNode metadata, Fetcher fetcher) throws IOException, InterruptedException {
        String url = ARXIV_API + "?search_query=" + encode("ti:\"" + text(metadata, "title") + "\"") + "&max_results=3";
        Response response = fetcher.get(url, "arxiv", "application/atom+xml");
        if (response.status() != 200) {
            return null;
        }
        Matcher entries = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL).matcher(response.text());
        while (entries.find()) {
            String entry = entries.group(1);
            Matcher title = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL).matcher(entry);
            Matcher summary = Pattern.compile("<summary>(.*?)</summary>", Pattern.DOTALL).matcher(entry);
            if (!title.find() || !summary.find() || !titlesMatch(text(metadata, "title"), cleanText(title.group(1)))) {
                continue;
            }
            String abstractText = usableAbstract(Entities.escape(cleanText(summary.group(1))));
            if (abstractText == null) {
                continue;
            }
            return new Found(abstractText, sourceRecord(uniqueSourceId(metadata), url, "other",
                    cleanText(title.group(1)), "arXiv", false, response.body()));
        }
        return null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodePath(String value) {
        return encode(value).replace("%2F", "/");
    }

    // ------------------------------------------------------------------
    // Runner
    // ------------------------------------------------------------------

    static final class Report {
        int considered;
        int skipped;
        int found;
        int missing;
        final Map<String, Integer> bySource = new TreeMap<>();
        final List<String> missingIds = new ArrayList<>();

        String summary() {
            StringBuilder sources = new StringBuilder();
            bySource.forEach((name, count) -> sources.append(sources.length() == 0 ? "" : ", ").append(name).append(' ').append(count));
            return String.format("Considered %d paper(s): %d abstract(s) stored (%s), %d already present, %d not found.",
                    considered, found, sources.length() == 0 ? "none" : sources, skipped, missing);
        }
    }

    static int run(List<String> arguments) throws IOException, InterruptedException {
        Main.Options options = new Main.Options(arguments);
        Path root = Path.of(options.require("root")).toAbsolutePath().normalize();
        Set<String> sources = new LinkedHashSet<>(Arrays.asList(options.get("sources", String.join(",", ALL_SOURCES)).split(",")));
        Report report = collect(root, options.get("venue", null), options.integer("year"), options.integer("limit"),
                options.flag("dry-run"), options.flag("force"), options.get("mailto", null), options.get("s2-api-key", null),
                sources, System.out::println);
        System.out.println(report.summary());
        return 0;
    }

    static Report collect(Path root, String venue, Integer year, Integer limit, boolean dryRun, boolean force,
                          String mailto, String s2ApiKey, Set<String> sources, Consumer<String> log)
            throws IOException, InterruptedException {
        Report report = new Report();
        Fetcher fetcher = new Fetcher(mailto, s2ApiKey, log);
        Set<String> stored = java.util.Collections.synchronizedSet(new LinkedHashSet<>());
        Object reportLock = new Object();

        List<Path> pending = new ArrayList<>();
        for (Path directory : Shards.paperDirectories(root)) {
            Path relative = root.relativize(directory);
            if (venue != null && !relative.getName(0).toString().equals(venue)) {
                continue;
            }
            if (year != null && !relative.getName(1).toString().equals(String.valueOf(year))) {
                continue;
            }
            ObjectNode metadata = JsonFiles.readObject(directory.resolve("metadata.json"));
            report.considered++;
            if (!text(metadata, "abstract").isEmpty() && !force) {
                report.skipped++;
                continue;
            }
            pending.add(directory.resolve("metadata.json"));
            if (limit != null && pending.size() >= limit) {
                break;
            }
        }

        Store store = (path, metadata, found, label) -> {
            synchronized (reportLock) {
                report.found++;
                report.bySource.merge(found.source().path("kind").asText(), 1, Integer::sum);
                stored.add(text(metadata, "paper_id"));
                log.accept(String.format("[%s] %s <- %s (%d chars)", label, text(metadata, "paper_id"),
                        found.source().path("kind").asText(), found.text().length()));
            }
            if (!dryRun) {
                JsonFiles.write(path, withAbstract(metadata, found.text(), found.source()));
            }
        };

        // Pass 1: official pages, one thread per site.
        Map<String, List<Path>> lanes = new LinkedHashMap<>();
        if (sources.contains("official")) {
            for (Path path : pending) {
                String lane = laneFor(JsonFiles.readObject(path));
                if (officialPage(lane) != null) {
                    lanes.computeIfAbsent(lane, key -> new ArrayList<>()).add(path);
                }
            }
        }
        List<Thread> threads = new ArrayList<>();
        for (Map.Entry<String, List<Path>> lane : lanes.entrySet()) {
            Thread thread = new Thread(() -> {
                Strategy strategy = officialPage(lane.getKey());
                List<Path> paths = lane.getValue();
                for (int index = 0; index < paths.size(); index++) {
                    String label = lane.getKey() + " " + (index + 1) + "/" + paths.size();
                    try {
                        ObjectNode metadata = JsonFiles.readObject(paths.get(index));
                        Found found = strategy.find(metadata, fetcher);
                        if (found != null) {
                            store.store(paths.get(index), metadata, found, label);
                        }
                    } catch (IOException exception) {
                        log.accept("[" + label + "] " + exception.getMessage() + "; deferred to the API passes");
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, lane.getKey());
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        // Pass 2: OpenAlex.
        if (sources.contains("openalex")) {
            runSequential("openalex", remaining(pending, stored), AbstractCollector::openAlex, fetcher, store, log);
        }

        // Pass 3: Semantic Scholar batches by DOI.
        if (sources.contains("s2-batch")) {
            List<Path> paths = new ArrayList<>();
            for (Path path : remaining(pending, stored)) {
                if (normalizeDoi(text(JsonFiles.readObject(path).path("links"), "doi")) != null) {
                    paths.add(path);
                }
            }
            int batches = (paths.size() + SEMANTIC_SCHOLAR_BATCH_SIZE - 1) / SEMANTIC_SCHOLAR_BATCH_SIZE;
            for (int start = 0; start < paths.size(); start += SEMANTIC_SCHOLAR_BATCH_SIZE) {
                List<Path> chunk = paths.subList(start, Math.min(start + SEMANTIC_SCHOLAR_BATCH_SIZE, paths.size()));
                String label = "s2-batch " + (start / SEMANTIC_SCHOLAR_BATCH_SIZE + 1) + "/" + batches;
                List<ObjectNode> batch = new ArrayList<>();
                for (Path path : chunk) {
                    batch.add(JsonFiles.readObject(path));
                }
                Map<String, Found> foundById;
                try {
                    foundById = semanticScholarBatch(batch, fetcher);
                } catch (IOException exception) {
                    log.accept("[" + label + "] " + exception.getMessage());
                    continue;
                }
                for (int index = 0; index < chunk.size(); index++) {
                    Found found = foundById.get(text(batch.get(index), "paper_id"));
                    if (found != null) {
                        store.store(chunk.get(index), batch.get(index), found, label);
                    }
                }
                log.accept("[" + label + "] " + foundById.size() + "/" + batch.size() + " abstracts");
            }
        }

        // Pass 4: Semantic Scholar title match; pass 5: arXiv.
        if (sources.contains("s2-match")) {
            runSequential("s2-match", remaining(pending, stored), AbstractCollector::semanticScholarMatch, fetcher, store, log);
        }
        if (sources.contains("arxiv")) {
            runSequential("arxiv", remaining(pending, stored), AbstractCollector::arxiv, fetcher, store, log);
        }

        for (Path path : remaining(pending, stored)) {
            String paperId = text(JsonFiles.readObject(path), "paper_id");
            report.missing++;
            report.missingIds.add(paperId);
            log.accept("no abstract found for " + paperId);
        }
        return report;
    }

    private interface Store {
        void store(Path path, ObjectNode metadata, Found found, String label) throws IOException;
    }

    private static List<Path> remaining(List<Path> pending, Set<String> stored) throws IOException {
        List<Path> paths = new ArrayList<>();
        for (Path path : pending) {
            if (!stored.contains(text(JsonFiles.readObject(path), "paper_id"))) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static void runSequential(String name, List<Path> paths, Strategy strategy, Fetcher fetcher,
                                      Store store, Consumer<String> log) throws IOException, InterruptedException {
        for (int index = 0; index < paths.size(); index++) {
            String label = name + " " + (index + 1) + "/" + paths.size();
            ObjectNode metadata = JsonFiles.readObject(paths.get(index));
            Found found;
            try {
                found = strategy.find(metadata, fetcher);
            } catch (IOException exception) {
                log.accept("[" + label + "] " + text(metadata, "paper_id") + ": " + exception.getMessage());
                continue;
            }
            if (found != null) {
                store.store(paths.get(index), metadata, found, label);
            }
        }
    }
}

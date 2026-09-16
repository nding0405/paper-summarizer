package org.systemsphd.explorer.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The classification vocabulary from {@code config/taxonomy.v1.json}: topic
 * tracks with their subtopics, system layers, and methods. Top-level tracks
 * carry a label; every other ID is a slug that is humanized for display, with
 * a short list of acronyms kept in upper case.
 */
public final class Taxonomy {
    /** One top-level topic track and its subtopic IDs, in taxonomy order. */
    public static final class Track {
        private final String id;
        private final String label;
        private final List<String> subtopics;

        Track(String id, String label, List<String> subtopics) {
            this.id = id;
            this.label = label;
            this.subtopics = List.copyOf(subtopics);
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public List<String> getSubtopics() {
            return subtopics;
        }
    }

    private static final Map<String, String> ACRONYMS = Map.of(
            "ml", "ML",
            "gpu", "GPU",
            "hpc", "HPC",
            "llm", "LLM",
            "rdma", "RDMA",
            "ssd", "SSD",
            "ebpf", "eBPF",
            "ddos", "DDoS"
    );

    private final List<Track> tracks;
    private final List<String> systemLayers;
    private final List<String> methods;
    private final Map<String, String> labels;

    private Taxonomy(List<Track> tracks, List<String> systemLayers, List<String> methods) {
        this.tracks = List.copyOf(tracks);
        this.systemLayers = List.copyOf(systemLayers);
        this.methods = List.copyOf(methods);
        Map<String, String> labels = new HashMap<>();
        for (Track track : tracks) {
            labels.put(track.getId(), track.getLabel());
        }
        this.labels = Map.copyOf(labels);
    }

    /** No structure; every label falls back to slug humanization. */
    public static Taxonomy empty() {
        return new Taxonomy(List.of(), List.of(), List.of());
    }

    public static Taxonomy load(InputStream stream) throws IOException {
        JsonNode root = new ObjectMapper().readTree(stream);
        List<Track> tracks = new ArrayList<>();
        for (JsonNode track : root.path("tracks")) {
            String id = track.path("id").asText("").trim();
            String label = track.path("label").asText("").trim();
            if (id.isEmpty()) {
                continue;
            }
            tracks.add(new Track(id, label.isEmpty() ? humanize(id) : label, strings(track.path("subtopics"))));
        }
        return new Taxonomy(tracks, strings(root.path("system_layers")), strings(root.path("methods")));
    }

    private static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public List<String> getSystemLayers() {
        return systemLayers;
    }

    public List<String> getMethods() {
        return methods;
    }

    public int size() {
        return labels.size();
    }

    public String labelFor(String id) {
        String label = labels.get(id);
        return label != null ? label : humanize(id);
    }

    /** "gpu-scheduling" becomes "GPU scheduling"; "memory_safety" becomes "Memory safety". */
    public static String humanize(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String[] words = id.trim().toLowerCase(Locale.ROOT).split("[-_\\s]+");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            String acronym = ACRONYMS.get(word);
            if (acronym != null) {
                label.append(acronym);
            } else if (label.length() == 0) {
                label.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
            } else {
                label.append(word);
            }
        }
        return label.toString();
    }
}

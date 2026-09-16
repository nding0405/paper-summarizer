package org.systemsphd.explorer.web;

import org.systemsphd.explorer.catalog.Taxonomy;
import org.systemsphd.explorer.model.Paper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Selectable keyword facets (topics, system layers, methods) with the number
 * of default-visible papers carrying each keyword. Built once per snapshot.
 */
public final class FacetIndex {
    public static final String TOPIC = "topic";
    public static final String LAYER = "layer";
    public static final String METHOD = "method";

    public static final class Option {
        private final String id;
        private final String label;
        private final long count;
        private final boolean subtopic;

        Option(String id, String label, long count, boolean subtopic) {
            this.id = id;
            this.label = label;
            this.count = count;
            this.subtopic = subtopic;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public long getCount() {
            return count;
        }

        /** Rendered indented under its track. */
        public boolean isSubtopic() {
            return subtopic;
        }
    }

    public static final class Facet {
        private final String key;
        private final String name;
        private final List<Option> options;
        private final Map<String, Option> byId;

        Facet(String key, String name, List<Option> options) {
            this.key = key;
            this.name = name;
            this.options = List.copyOf(options);
            Map<String, Option> byId = new HashMap<>();
            options.forEach(option -> byId.put(option.getId(), option));
            this.byId = Map.copyOf(byId);
        }

        /** Request parameter name, e.g. "topic". */
        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public List<Option> getOptions() {
            return options;
        }

        public boolean has(String id) {
            return byId.containsKey(id);
        }

        public String labelFor(String id) {
            Option option = byId.get(id);
            return option == null ? Taxonomy.humanize(id) : option.getLabel();
        }
    }

    private final Map<String, Facet> facets;

    private FacetIndex(Map<String, Facet> facets) {
        this.facets = Map.copyOf(facets);
    }

    public static FacetIndex empty() {
        return new FacetIndex(Map.of(
                TOPIC, new Facet(TOPIC, "Topics", List.of()),
                LAYER, new Facet(LAYER, "System layers", List.of()),
                METHOD, new Facet(METHOD, "Methods", List.of())
        ));
    }

    /** Counts keywords over the given papers (callers pass the default-visible set). */
    public static FacetIndex build(List<Paper> papers, Taxonomy taxonomy) {
        Map<String, Long> topicCounts = count(papers, Paper::getTopics);
        Map<String, Long> layerCounts = count(papers, Paper::getSystemLayers);
        Map<String, Long> methodCounts = count(papers, Paper::getMethods);

        List<Option> topics = new ArrayList<>();
        Map<String, Long> remaining = new TreeMap<>(topicCounts);
        for (Taxonomy.Track track : taxonomy.getTracks()) {
            long trackCount = remaining.getOrDefault(track.getId(), 0L);
            List<Option> subtopics = new ArrayList<>();
            for (String subtopic : track.getSubtopics()) {
                long count = remaining.getOrDefault(subtopic, 0L);
                if (count > 0) {
                    subtopics.add(new Option(subtopic, Taxonomy.humanize(subtopic), count, true));
                }
                remaining.remove(subtopic);
            }
            remaining.remove(track.getId());
            if (trackCount > 0 || !subtopics.isEmpty()) {
                topics.add(new Option(track.getId(), track.getLabel(), trackCount, false));
                topics.addAll(subtopics);
            }
        }
        // Keywords used by classifiers but absent from the taxonomy, alphabetically.
        remaining.forEach((id, count) -> topics.add(new Option(id, Taxonomy.humanize(id), count, false)));

        Map<String, Facet> facets = new LinkedHashMap<>();
        facets.put(TOPIC, new Facet(TOPIC, "Topics", topics));
        facets.put(LAYER, new Facet(LAYER, "System layers", ordered(taxonomy.getSystemLayers(), layerCounts)));
        facets.put(METHOD, new Facet(METHOD, "Methods", ordered(taxonomy.getMethods(), methodCounts)));
        return new FacetIndex(facets);
    }

    private static Map<String, Long> count(List<Paper> papers, Function<Paper, List<String>> values) {
        Map<String, Long> counts = new HashMap<>();
        for (Paper paper : papers) {
            for (String value : values.apply(paper)) {
                counts.merge(value, 1L, Long::sum);
            }
        }
        return counts;
    }

    /** Taxonomy order first, then any corpus-only values alphabetically; zero counts are dropped. */
    private static List<Option> ordered(List<String> taxonomyOrder, Map<String, Long> counts) {
        List<Option> options = new ArrayList<>();
        Map<String, Long> remaining = new TreeMap<>(counts);
        for (String id : taxonomyOrder) {
            long count = remaining.getOrDefault(id, 0L);
            remaining.remove(id);
            if (count > 0) {
                options.add(new Option(id, Taxonomy.humanize(id), count, false));
            }
        }
        remaining.forEach((id, count) -> options.add(new Option(id, Taxonomy.humanize(id), count, false)));
        return options;
    }

    public List<Facet> getFacets() {
        return List.copyOf(facets.values());
    }

    public Facet get(String key) {
        return facets.get(key);
    }

    public boolean isValid(String key, String id) {
        Facet facet = facets.get(key);
        return facet != null && facet.has(id);
    }

    public String labelFor(String key, String id) {
        Facet facet = facets.get(key);
        return facet == null ? Taxonomy.humanize(id) : facet.labelFor(id);
    }
}

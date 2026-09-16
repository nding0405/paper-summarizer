package org.systemsphd.explorer.catalog;

import org.junit.jupiter.api.Test;
import org.systemsphd.explorer.data.PaperRepository;
import org.systemsphd.explorer.model.Paper;
import org.systemsphd.explorer.support.ExportBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxonomyTest {
    private static final Path TAXONOMY = Path.of("config", "taxonomy.v1.json");

    @Test
    void usesTrackLabelsFromTheRepositoryTaxonomy() throws IOException {
        Taxonomy labels;
        try (InputStream stream = Files.newInputStream(TAXONOMY)) {
            labels = Taxonomy.load(stream);
        }

        assertTrue(labels.size() >= 20);
        assertEquals("ML Systems", labels.labelFor("ml-systems"));
        assertEquals("Compilers, Languages, and Runtimes", labels.labelFor("compilers-languages-and-runtimes"));
        assertEquals("Operating Systems and Kernels", labels.labelFor("operating-systems-and-kernels"));
    }

    @Test
    void humanizesSubtopicSlugsAndKeepsAcronymsUpperCase() {
        Taxonomy labels = Taxonomy.empty();

        assertEquals("Kernel extensibility", labels.labelFor("kernel-extensibility"));
        assertEquals("GPU scheduling", labels.labelFor("gpu-scheduling"));
        assertEquals("SSD and flash", labels.labelFor("ssd-and-flash"));
        assertEquals("Distributed ML", labels.labelFor("distributed-ml"));
        assertEquals("eBPF", labels.labelFor("ebpf"));
        assertEquals("Memory safety", labels.labelFor("memory_safety"));
        assertEquals("", Taxonomy.humanize("  "));
    }

    @Test
    void papersCarryAtMostTwoLabelsInClassifierOrder() throws IOException {
        Taxonomy labels;
        try (InputStream stream = Files.newInputStream(TAXONOMY)) {
            labels = Taxonomy.load(stream);
        }
        ExportBuilder export = new ExportBuilder()
                .paper("p", "osdi", 2025).topics("ml-systems", "gpu-scheduling", "linux").add();
        Paper paper = new PaperRepository(labels)
                .load(new ByteArrayInputStream(export.toBytes()), "test")
                .getPapers().get(0);

        assertEquals(List.of("ML Systems", "GPU scheduling"), paper.getTopicLabels());
        assertEquals(List.of("ml-systems", "gpu-scheduling", "linux"), paper.getTopics());
    }
}

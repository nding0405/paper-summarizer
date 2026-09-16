package org.systemsphd.explorer.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Locates paper bundles and venue-year files under a shards root. */
final class Shards {
    static final List<String> STAGE_FILES = List.of(
            "metadata.json", "relevance.json", "summary.json", "groups.json", "awards.json", "review.json"
    );

    private Shards() {
    }

    /** Paper directories (those containing metadata.json), in a stable sorted order. */
    static List<Path> paperDirectories(Path root) throws IOException {
        List<Path> directories = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return directories;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> path.getFileName().toString().equals("metadata.json"))
                    .map(Path::getParent)
                    .sorted()
                    .forEach(directories::add);
        }
        return directories;
    }

    /** venue-year.json files, in a stable sorted order. */
    static List<Path> venueYearFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return files;
        }
        try (Stream<Path> stream = Files.walk(root, 3)) {
            stream.filter(path -> path.getFileName().toString().equals("venue-year.json"))
                    .sorted()
                    .forEach(files::add);
        }
        return files;
    }

    /** Repository root: the directory holding config/, schemas/, and data/. */
    static Path repositoryRoot(String override) {
        if (override != null) {
            return Path.of(override).toAbsolutePath().normalize();
        }
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve("schemas")) && Files.isDirectory(candidate.resolve("config"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}

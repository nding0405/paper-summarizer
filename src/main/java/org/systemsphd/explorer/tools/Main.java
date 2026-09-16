package org.systemsphd.explorer.tools;

import java.util.Arrays;
import java.util.List;

/**
 * Command-line entry point for the data tools:
 *
 * <pre>
 *   init-paper        create a paper bundle with a deterministic ID
 *   init-venue        create a venue-year record
 *   validate          check every shard and entity file against the JSON schemas
 *   export            build site/data/papers.json and venue-years.json from the shards
 *   collect-abstracts fetch published abstracts into metadata.json
 * </pre>
 *
 * Run through Maven: {@code ./mvnw -q compile exec:java -Dexec.args="validate --root data/shards"}.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 0) {
            usage();
            System.exit(2);
        }
        List<String> rest = Arrays.asList(arguments).subList(1, arguments.length);
        int status = switch (arguments[0]) {
            case "init-paper" -> Initialize.runInitPaper(rest);
            case "init-venue" -> Initialize.runInitVenue(rest);
            case "export" -> ShardExport.run(rest);
            case "validate" -> Validator.run(rest);
            case "collect-abstracts" -> AbstractCollector.run(rest);
            case "-h", "--help", "help" -> {
                usage();
                yield 0;
            }
            default -> {
                System.err.println("Unknown command: " + arguments[0]);
                usage();
                yield 2;
            }
        };
        System.exit(status);
    }

    private static void usage() {
        System.err.println("""
                Usage: <command> [options]

                  init-paper --venue osdi --year 2025 --title "Exact Title" --owner agent-a [--root data/shards]
                  init-venue --venue sosp --year 2026 --owner agent-a [--root data/shards]
                  validate --root data/shards [--repo .]
                  export --root data/shards --output site/data [--repo .]
                  collect-abstracts --root data/shards [--venue V] [--year Y] [--limit N]
                                    [--dry-run] [--force] [--mailto EMAIL] [--s2-api-key KEY]
                                    [--sources official,openalex,s2-batch,s2-match,arxiv]
                """);
    }

    /** Minimal {@code --name value} / {@code --flag} parsing shared by the tools. */
    static final class Options {
        private final java.util.Map<String, String> values = new java.util.LinkedHashMap<>();

        Options(List<String> arguments) {
            for (int index = 0; index < arguments.size(); index++) {
                String argument = arguments.get(index);
                if (!argument.startsWith("--")) {
                    throw new IllegalArgumentException("Unexpected argument: " + argument);
                }
                String name = argument.substring(2);
                if (index + 1 < arguments.size() && !arguments.get(index + 1).startsWith("--")) {
                    values.put(name, arguments.get(++index));
                } else {
                    values.put(name, "true");
                }
            }
        }

        String get(String name, String fallback) {
            return values.getOrDefault(name, fallback);
        }

        String require(String name) {
            String value = values.get(name);
            if (value == null || "true".equals(value)) {
                throw new IllegalArgumentException("Missing --" + name);
            }
            return value;
        }

        boolean flag(String name) {
            return "true".equals(values.get(name));
        }

        Integer integer(String name) {
            String value = values.get(name);
            return value == null || "true".equals(value) ? null : Integer.valueOf(value);
        }
    }
}

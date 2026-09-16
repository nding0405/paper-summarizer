package org.systemsphd.explorer.support;

import org.systemsphd.explorer.catalog.ConferenceCatalog;
import org.systemsphd.explorer.web.FacetIndex;
import org.systemsphd.explorer.web.FilterState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds a {@link FilterState} from literal query parameters; repeated names become multiple values. */
public final class Filters {
    private static final ConferenceCatalog CATALOG = new ConferenceCatalog();

    private Filters() {
    }

    public static FilterState of(String... keyValues) {
        return build("/index.jsp", FacetIndex.empty(), keyValues);
    }

    public static FilterState at(String basePath, String... keyValues) {
        return build(basePath, FacetIndex.empty(), keyValues);
    }

    /** Uses a facet index so keyword parameters are accepted. */
    public static FilterState with(FacetIndex facets, String... keyValues) {
        return build("/index.jsp", facets, keyValues);
    }

    private static FilterState build(String basePath, FacetIndex facets, String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key/value pairs");
        }
        Map<String, List<String>> parameters = new HashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            parameters.computeIfAbsent(keyValues[index], key -> new ArrayList<>()).add(keyValues[index + 1]);
        }
        return FilterState.from(
                name -> {
                    List<String> values = parameters.get(name);
                    return values == null ? null : values.toArray(String[]::new);
                },
                CATALOG,
                facets,
                basePath
        );
    }
}

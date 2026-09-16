package org.systemsphd.explorer.catalog;

import java.util.List;

public final class ConferenceCategory {
    private final String slug;
    private final String name;
    private final List<ConferenceDefinition> conferences;

    public ConferenceCategory(String slug, String name, List<ConferenceDefinition> conferences) {
        this.slug = slug;
        this.name = name;
        this.conferences = List.copyOf(conferences);
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public List<ConferenceDefinition> getConferences() {
        return conferences;
    }
}

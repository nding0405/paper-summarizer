package org.systemsphd.explorer.catalog;

public final class ConferenceDefinition {
    private final String slug;
    private final String name;
    private final String fullName;
    private final String categorySlug;

    public ConferenceDefinition(String slug, String name, String fullName, String categorySlug) {
        this.slug = slug;
        this.name = name;
        this.fullName = fullName;
        this.categorySlug = categorySlug;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCategorySlug() {
        return categorySlug;
    }
}

package org.systemsphd.explorer.web;

import java.util.List;

public final class NavigationVenue {
    private final String slug;
    private final String name;
    private final String fullName;
    private final long count;
    private final String href;
    private final boolean selected;
    private final List<NavigationYear> years;

    public NavigationVenue(
            String slug,
            String name,
            String fullName,
            long count,
            String href,
            boolean selected,
            List<NavigationYear> years
    ) {
        this.slug = slug;
        this.name = name;
        this.fullName = fullName;
        this.count = count;
        this.href = href;
        this.selected = selected;
        this.years = List.copyOf(years);
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

    public long getCount() {
        return count;
    }

    public String getHref() {
        return href;
    }

    public boolean isSelected() {
        return selected;
    }

    public List<NavigationYear> getYears() {
        return years;
    }
}

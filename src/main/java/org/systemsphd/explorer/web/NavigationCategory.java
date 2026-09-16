package org.systemsphd.explorer.web;

import java.util.List;

public final class NavigationCategory {
    private final String slug;
    private final String name;
    private final long count;
    private final String href;
    private final boolean selected;
    private final List<NavigationVenue> venues;

    public NavigationCategory(
            String slug,
            String name,
            long count,
            String href,
            boolean selected,
            List<NavigationVenue> venues
    ) {
        this.slug = slug;
        this.name = name;
        this.count = count;
        this.href = href;
        this.selected = selected;
        this.venues = List.copyOf(venues);
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
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

    public List<NavigationVenue> getVenues() {
        return venues;
    }
}

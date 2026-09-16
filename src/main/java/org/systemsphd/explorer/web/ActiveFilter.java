package org.systemsphd.explorer.web;

/** One applied filter shown above the paper list, with a link that removes it. */
public final class ActiveFilter {
    private final String name;
    private final String value;
    private final String removeHref;

    public ActiveFilter(String name, String value, String removeHref) {
        this.name = name;
        this.value = value;
        this.removeHref = removeHref;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getRemoveHref() {
        return removeHref;
    }
}

package org.systemsphd.explorer.web;

/** A link that moves several pages at once, e.g. "+10". */
public final class PageJump {
    private final String label;
    private final int page;
    private final String href;

    public PageJump(String label, int page, String href) {
        this.label = label;
        this.page = page;
        this.href = href;
    }

    public String getLabel() {
        return label;
    }

    public int getPage() {
        return page;
    }

    public String getHref() {
        return href;
    }
}

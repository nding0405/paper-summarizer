package org.systemsphd.explorer.web;

import org.systemsphd.explorer.model.Paper;

import java.util.List;

/** One track or session of a conference program with its papers. */
public final class ProceedingsSession {
    private final String name;
    private final String anchor;
    private final List<Paper> papers;

    public ProceedingsSession(String name, String anchor, List<Paper> papers) {
        this.name = name;
        this.anchor = anchor;
        this.papers = List.copyOf(papers);
    }

    public String getName() {
        return name;
    }

    /** Fragment identifier used by the table of contents. */
    public String getAnchor() {
        return anchor;
    }

    public List<Paper> getPapers() {
        return papers;
    }

    public int getCount() {
        return papers.size();
    }
}

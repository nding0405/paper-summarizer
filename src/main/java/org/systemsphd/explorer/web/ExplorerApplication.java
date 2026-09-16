package org.systemsphd.explorer.web;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.systemsphd.explorer.catalog.ConferenceCatalog;
import org.systemsphd.explorer.catalog.Taxonomy;
import org.systemsphd.explorer.catalog.VenueCoverage;
import org.systemsphd.explorer.data.PaperRepository;
import org.systemsphd.explorer.model.RepositorySnapshot;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads the packaged data once at startup and shares one
 * {@link ExplorerService} between the search page and the proceedings page.
 */
@WebListener
public final class ExplorerApplication implements ServletContextListener {
    static final String SERVICE_ATTRIBUTE = ExplorerService.class.getName();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        try {
            Taxonomy taxonomy = loadTaxonomy(context);
            RepositorySnapshot snapshot = new PaperRepository(taxonomy).load(context);
            VenueCoverage coverage = loadCoverage(context);
            ExplorerService service = new ExplorerService(snapshot, new ConferenceCatalog(), coverage, taxonomy);
            context.setAttribute(SERVICE_ATTRIBUTE, service);
            context.log("Loaded " + snapshot.getPapers().size() + " papers from " + snapshot.getSourceLabel()
                    + " and coverage for " + coverage.size() + " venue-years");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the paper index", exception);
        }
    }

    static ExplorerService service(ServletContext context) {
        ExplorerService service = (ExplorerService) context.getAttribute(SERVICE_ATTRIBUTE);
        if (service == null) {
            throw new IllegalStateException("The paper index was not loaded at startup");
        }
        return service;
    }

    private static Taxonomy loadTaxonomy(ServletContext context) throws IOException {
        try (InputStream stream = context.getResourceAsStream("/WEB-INF/data/taxonomy.v1.json")) {
            return stream == null ? Taxonomy.empty() : Taxonomy.load(stream);
        }
    }

    private static VenueCoverage loadCoverage(ServletContext context) throws IOException {
        try (InputStream stream = context.getResourceAsStream("/WEB-INF/data/venue-years.json")) {
            return stream == null ? VenueCoverage.empty() : VenueCoverage.load(stream);
        }
    }
}

package org.systemsphd.explorer.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.systemsphd.explorer.catalog.ConferenceCatalog;
import org.systemsphd.explorer.model.Paper;

import java.io.IOException;

/** Serves {@code /paper.jsp?id=...}: one paper with its abstract, summary, and review. */
@WebServlet(urlPatterns = PaperServlet.PAGE)
public final class PaperServlet extends HttpServlet {
    static final String PAGE = "/paper.jsp";

    private ExplorerService explorerService;

    @Override
    public void init() throws ServletException {
        explorerService = ExplorerApplication.service(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecurityHeaders.apply(response);
        Paper paper = explorerService.find(request.getParameter("id"));
        if (paper == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ConferenceCatalog catalog = explorerService.getCatalog();
        String context = request.getContextPath();
        String proceedingsUrl = context + ProceedingsServlet.PAGE + "?venue=" + paper.getVenue() + "&year=" + paper.getYear();
        String searchUrl = context + ExplorerServlet.PAGE + "?category=" + catalog.categoryForVenue(paper.getVenue())
                + "&venue=" + paper.getVenue() + "&year=" + paper.getYear() + "&relevance=ALL";
        request.setAttribute("view", new PaperPage(
                paper, catalog.venueName(paper.getVenue()), catalog.venueFullName(paper.getVenue()),
                proceedingsUrl, searchUrl, explorerService.venueNames()
        ));
        request.setAttribute("searchPath", context + ExplorerServlet.PAGE);
        request.setAttribute("proceedingsPath", context + ProceedingsServlet.PAGE);
        request.setAttribute("stats", explorerService.getStatistics());
        request.getRequestDispatcher("/WEB-INF/views/paper.jsp").forward(request, response);
    }
}

package org.systemsphd.explorer.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Serves {@code /proceedings.jsp}: one conference-year laid out like its
 * program, grouped by track or session, for reading straight through.
 */
@WebServlet(urlPatterns = ProceedingsServlet.PAGE)
public final class ProceedingsServlet extends HttpServlet {
    static final String PAGE = "/proceedings.jsp";

    private ExplorerService explorerService;

    @Override
    public void init() throws ServletException {
        explorerService = ExplorerApplication.service(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecurityHeaders.apply(response);
        ProceedingsPage view = explorerService.proceedings(
                request.getParameter("venue"),
                request.getParameter("year"),
                request.getContextPath() + PAGE,
                request.getContextPath() + ExplorerServlet.PAGE
        );
        request.setAttribute("view", view);
        request.setAttribute("basePath", request.getContextPath() + PAGE);
        request.setAttribute("searchPath", request.getContextPath() + ExplorerServlet.PAGE);
        request.setAttribute("stats", explorerService.getStatistics());
        request.getRequestDispatcher("/WEB-INF/views/proceedings.jsp").forward(request, response);
    }
}

package org.systemsphd.explorer.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Serves the search page at {@code /index.jsp}. The exact mapping takes
 * priority over the container's {@code *.jsp} mapping, so the view stays under
 * {@code WEB-INF} while the address bar shows the familiar file name. A request
 * for the context root redirects there; static files under {@code /assets/}
 * stay with Tomcat's default servlet.
 */
@WebServlet(urlPatterns = {"", ExplorerServlet.PAGE})
public final class ExplorerServlet extends HttpServlet {
    static final String PAGE = "/index.jsp";

    private ExplorerService explorerService;

    @Override
    public void init() throws ServletException {
        explorerService = ExplorerApplication.service(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!PAGE.equals(request.getServletPath())) {
            String query = request.getQueryString();
            response.sendRedirect(request.getContextPath() + PAGE + (query == null ? "" : "?" + query));
            return;
        }

        SecurityHeaders.apply(response);
        FilterState filters = FilterState.from(
                request::getParameterValues,
                explorerService.getCatalog(),
                explorerService.getFacets(),
                request.getContextPath() + PAGE
        );
        ExplorerPage view = explorerService.page(filters);

        request.setAttribute("view", view);
        request.setAttribute("facets", explorerService.getFacets());
        request.setAttribute("proceedingsPath", request.getContextPath() + ProceedingsServlet.PAGE);
        request.setAttribute("stats", explorerService.getStatistics());
        request.getRequestDispatcher("/WEB-INF/views/index.jsp").forward(request, response);
    }
}

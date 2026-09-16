package org.systemsphd.explorer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.systemsphd.explorer.model.Paper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JSON suggestions for the search box: the best-ranked papers for a partial
 * query, each with a link to its page. Used by assets/app.js; the search form
 * works without it.
 */
@WebServlet(urlPatterns = SuggestServlet.PATH)
public final class SuggestServlet extends HttpServlet {
    static final String PATH = "/suggest.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExplorerService explorerService;

    @Override
    public void init() throws ServletException {
        explorerService = ExplorerApplication.service(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String query = request.getParameter("q");
        if (query != null && query.length() > FilterState.MAX_QUERY_LENGTH) {
            query = query.substring(0, FilterState.MAX_QUERY_LENGTH);
        }
        Map<String, String> venueNames = explorerService.venueNames();
        String paperPath = request.getContextPath() + PaperServlet.PAGE + "?id=";

        ArrayNode suggestions = MAPPER.createArrayNode();
        for (Paper paper : explorerService.suggest(query)) {
            ObjectNode item = suggestions.addObject();
            item.put("id", paper.getId());
            item.put("title", paper.getTitle());
            item.put("venue", venueNames.getOrDefault(paper.getVenue(), paper.getVenue()));
            item.put("year", paper.getYear());
            item.put("authors", paper.getAuthorLine());
            item.put("url", paperPath + URLEncoder.encode(paper.getId(), StandardCharsets.UTF_8));
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        MAPPER.writeValue(response.getWriter(), suggestions);
    }
}

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="en-US" scope="request" />
<c:set var="filters" value="${view.filters}" />
<c:set var="base" value="${filters.basePath}" />
<c:set var="assets" value="${pageContext.request.contextPath}/assets" />
<c:set var="paperPath" value="${pageContext.request.contextPath}/paper.jsp" />
<c:set var="searchPath" value="${base}" />
<c:set var="suggestPath" value="${pageContext.request.contextPath}/suggest.json" />
<c:set var="venueNames" value="${view.venueNames}" />
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><c:out value="${view.heading}" /></title>
<link rel="stylesheet" href="<c:out value='${assets}' />/app.css">
</head>
<body>
<a class="skip-link" href="#papers">Skip to papers</a>

<header class="topbar">
  <div class="topbar-left">
    <%@ include file="site-home.jspf" %>
    <form class="filter-form" method="get" action="<c:out value='${base}' />">
      <c:if test="${not empty filters.query}">
        <input type="hidden" name="q" value="<c:out value='${filters.query}' />">
      </c:if>
      <c:if test="${not empty filters.category and empty filters.venue}">
        <input type="hidden" name="category" value="<c:out value='${filters.category}' />">
      </c:if>
      <details class="filter-popup">
        <summary class="button button-secondary">
          Filters<c:if test="${view.filterCount > 0}"> <span class="filter-count">${view.filterCount}</span></c:if>
        </summary>
        <div class="filter-panel">
          <div class="filter-panel-grid">
            <label class="field">
              <span class="field-label">Conference</span>
              <select name="venue">
                <option value="">All</option>
                <c:forEach var="category" items="${view.categories}">
                  <optgroup label="<c:out value='${category.name}' />">
                    <c:forEach var="conference" items="${category.conferences}">
                      <option value="<c:out value='${conference.slug}' />"${conference.slug == filters.venue ? ' selected' : ''}><c:out value="${conference.name}" /></option>
                    </c:forEach>
                  </optgroup>
                </c:forEach>
              </select>
            </label>
            <label class="field">
              <span class="field-label">Year</span>
              <select name="year">
                <option value="">All</option>
                <c:forEach var="year" items="${view.years}">
                  <option value="${year}"${year == filters.year ? ' selected' : ''}>${year}</option>
                </c:forEach>
              </select>
            </label>
            <label class="field">
              <span class="field-label">Relevance</span>
              <select name="relevance">
                <option value="">Default (hide excluded)</option>
                <option value="ALL"${filters.relevance == 'ALL' ? ' selected' : ''}>All classes</option>
                <option value="CORE_SYSTEMS"${filters.relevance == 'CORE_SYSTEMS' ? ' selected' : ''}>Core systems</option>
                <option value="SYSTEMS_ADJACENT"${filters.relevance == 'SYSTEMS_ADJACENT' ? ' selected' : ''}>Systems-adjacent</option>
                <option value="NEEDS_HUMAN_REVIEW"${filters.relevance == 'NEEDS_HUMAN_REVIEW' ? ' selected' : ''}>Needs review</option>
                <option value="EXCLUDED"${filters.relevance == 'EXCLUDED' ? ' selected' : ''}>Excluded only</option>
              </select>
            </label>
            <label class="field">
              <span class="field-label">Has</span>
              <select name="availability">
                <option value="">Anything</option>
                <option value="summary"${filters.availability == 'summary' ? ' selected' : ''}>LLM summary</option>
                <option value="abstract"${filters.availability == 'abstract' ? ' selected' : ''}>Abstract</option>
                <option value="code"${filters.availability == 'code' ? ' selected' : ''}>Code</option>
                <option value="artifact"${filters.availability == 'artifact' ? ' selected' : ''}>Artifact</option>
                <option value="either"${filters.availability == 'either' ? ' selected' : ''}>Code or artifact</option>
              </select>
            </label>
          </div>

          <c:forEach var="facet" items="${facets.facets}">
            <c:if test="${not empty facet.options}">
              <c:set var="selectedIds" value="${filters.selected(facet.key)}" />
              <fieldset class="facet facet-${facet.key}">
                <legend><c:out value="${facet.name}" /></legend>
                <div class="facet-options">
                  <c:forEach var="option" items="${facet.options}">
                    <label class="facet-option${option.subtopic ? ' is-subtopic' : ''}">
                      <input type="checkbox" name="<c:out value='${facet.key}' />" value="<c:out value='${option.id}' />"${selectedIds.contains(option.id) ? ' checked' : ''}>
                      <span class="facet-label"><c:out value="${option.label}" /></span>
                      <span class="count"><fmt:formatNumber value="${option.count}" /></span>
                    </label>
                  </c:forEach>
                </div>
              </fieldset>
            </c:if>
          </c:forEach>

          <fieldset class="facet-match">
            <legend>Selected keywords</legend>
            <label><input type="radio" name="match" value="any"${filters.matchAll ? '' : ' checked'}> match any</label>
            <label><input type="radio" name="match" value="all"${filters.matchAll ? ' checked' : ''}> match all</label>
            <span class="muted">Topics, layers, and methods always combine with each other.</span>
          </fieldset>

          <div class="filter-panel-actions">
            <button type="submit" class="button">Apply filters</button>
            <a class="button button-secondary" href="<c:out value='${base}' />">Reset</a>
          </div>
        </div>
      </details>
    </form>
  </div>
  <div class="topbar-right">
    <c:set var="searchQuery" value="${filters.query}" />
    <c:set var="searchHidden" value="${filters.hiddenParameters}" />
    <%@ include file="site-search.jspf" %>
    <c:set var="currentPage" value="search" />
    <%@ include file="site-nav.jspf" %>
  </div>
</header>

<div class="layout">
  <nav class="sidebar" aria-label="Conferences">
    <ul class="nav-root">
      <li>
        <a class="nav-all" href="<c:out value='${filters.urlFor("", "", null)}' />"
           <c:if test="${empty filters.category and empty filters.venue}">aria-current="page"</c:if>>
          <span>All conferences</span>
          <span class="count"><fmt:formatNumber value="${view.navigationTotal}" /></span>
        </a>
      </li>
      <c:forEach var="category" items="${view.navigation}">
        <li class="nav-category">
          <a class="nav-category-link" href="<c:out value='${category.href}' />"
             <c:if test="${category.selected}">aria-current="page"</c:if>>
            <span><c:out value="${category.name}" /></span>
            <span class="count"><fmt:formatNumber value="${category.count}" /></span>
          </a>
          <ul class="nav-venues">
            <c:forEach var="venue" items="${category.venues}">
              <li class="nav-venue${venue.slug == filters.venue ? ' is-current' : ''}">
                <a class="nav-venue-link" href="<c:out value='${venue.href}' />"
                   title="<c:out value='${venue.fullName}' />"
                   <c:if test="${venue.selected}">aria-current="page"</c:if>>
                  <span><c:out value="${venue.name}" /></span>
                  <span class="count"><fmt:formatNumber value="${venue.count}" /></span>
                </a>
                <c:if test="${not empty venue.years}">
                  <ul class="nav-years">
                    <c:forEach var="year" items="${venue.years}">
                      <li>
                        <a href="<c:out value='${year.href}' />"
                           <c:if test="${year.selected}">aria-current="page"</c:if>
                           <c:if test="${not empty year.mark}">title="<c:out value='${year.coverage.label}' />"</c:if>>
                          <span>${year.year}</span>
                          <c:if test="${year.showCount}">
                            <span class="count"><fmt:formatNumber value="${year.count}" /></span>
                          </c:if>
                          <c:if test="${not empty year.mark}">
                            <span class="coverage-mark"><c:out value="${year.mark}" /></span>
                          </c:if>
                        </a>
                      </li>
                    </c:forEach>
                  </ul>
                </c:if>
              </li>
            </c:forEach>
          </ul>
        </li>
      </c:forEach>
    </ul>
  </nav>

  <main id="papers" class="content">
    <c:if test="${view.fixture}">
      <p class="notice" role="status">
        Showing <strong>synthetic fixture data</strong>: every paper, person, and group below is invented
        for interface testing. Run <code>make export</code> and rebuild to package the real export.
      </p>
    </c:if>

    <div class="results-header">
      <h1 class="results-heading"><c:out value="${view.heading}" /></h1>
      <c:if test="${not empty filters.venue and filters.year != null}">
        <a class="results-proceedings" href="<c:out value='${proceedingsPath}' />?venue=<c:out value='${filters.venue}' />&amp;year=<c:out value='${filters.yearValue}' />">Read as proceedings, by session &rarr;</a>
      </c:if>
      <p class="results-count" role="status">
        <c:choose>
          <c:when test="${view.noProgram}"><c:out value="${view.coverage.label}" /></c:when>
          <c:when test="${view.totalResults == 0}">No papers</c:when>
          <c:when test="${view.totalResults == 1}">1 paper</c:when>
          <c:otherwise>
            Showing <fmt:formatNumber value="${view.firstResult}" />–<fmt:formatNumber value="${view.lastResult}" /> of <fmt:formatNumber value="${view.totalResults}" /> papers
          </c:otherwise>
        </c:choose>
      </p>
    </div>

    <c:if test="${view.coverage != null and not view.coverage.complete}">
      <p class="notice notice-coverage" role="status">
        Coverage for <c:out value="${view.heading}" />: <strong><c:out value="${view.coverage.label}" /></strong>.
        <c:if test="${not empty view.coverage.lastVerifiedDate}">
          Last verified <c:out value="${view.coverage.lastVerifiedDate}" />.
        </c:if>
        Collection status is not a relevance or quality judgement.
      </p>
    </c:if>

    <c:if test="${not empty view.activeFilters}">
      <ul class="active-filters" aria-label="Active filters">
        <c:forEach var="filter" items="${view.activeFilters}">
          <li class="active-filter">
            <span class="active-filter-name"><c:out value="${filter.name}" />:</span>
            <span class="active-filter-value"><c:out value="${filter.value}" /></span>
            <a class="active-filter-remove" href="<c:out value='${filter.removeHref}' />"
               aria-label="Remove <c:out value='${filter.name}' /> filter">&#x2715;</a>
          </li>
        </c:forEach>
        <li><a class="active-filters-clear" href="<c:out value='${filters.clearUrl()}' />">Clear all</a></li>
      </ul>
    </c:if>

    <c:choose>
      <c:when test="${view.noProgram}">
        <div class="empty-state">
          <p><c:out value="${view.heading}" />: <c:out value="${view.coverage.label}" />, so there is nothing to search yet.</p>
          <p>Pick another year in the sidebar or
            <a href="<c:out value='${filters.clearUrl()}' />">show all papers</a>.</p>
        </div>
      </c:when>
      <c:when test="${empty view.papers}">
        <div class="empty-state">
          <p>No papers match these filters.</p>
          <p>Try fewer search terms, another conference or year, or
            <a href="<c:out value='${filters.clearUrl()}' />">show all papers</a>.</p>
        </div>
      </c:when>
      <c:otherwise>
        <ol class="paper-list" start="${view.firstResult}">
          <c:forEach var="paper" items="${view.papers}">
            <%@ include file="paper-entry.jspf" %>
          </c:forEach>
        </ol>

        <c:if test="${view.totalPages > 1}">
          <nav class="pagination" aria-label="Pagination">
            <div class="pagination-links">
              <c:forEach var="jump" items="${view.backwardJumps}">
                <a href="<c:out value='${jump.href}' />" aria-label="Back ${jump.page == view.currentPage - 5 ? 5 : 10} pages, to page ${jump.page}"><c:out value="${jump.label}" /></a>
              </c:forEach>
              <c:choose>
                <c:when test="${view.hasPrevious}">
                  <a href="<c:out value='${view.previousUrl}' />" rel="prev">&larr; Previous</a>
                </c:when>
                <c:otherwise><span class="pagination-disabled">&larr; Previous</span></c:otherwise>
              </c:choose>
              <span class="pagination-status">Page <fmt:formatNumber value="${view.currentPage}" /> of <fmt:formatNumber value="${view.totalPages}" /></span>
              <c:choose>
                <c:when test="${view.hasNext}">
                  <a href="<c:out value='${view.nextUrl}' />" rel="next">Next &rarr;</a>
                </c:when>
                <c:otherwise><span class="pagination-disabled">Next &rarr;</span></c:otherwise>
              </c:choose>
              <c:forEach var="jump" items="${view.forwardJumps}">
                <a href="<c:out value='${jump.href}' />" aria-label="Forward ${jump.page == view.currentPage + 5 ? 5 : 10} pages, to page ${jump.page}"><c:out value="${jump.label}" /></a>
              </c:forEach>
            </div>
            <form class="pagination-jump" method="get" action="<c:out value='${base}' />">
              <c:forEach var="parameter" items="${filters.hiddenParameters}">
                <input type="hidden" name="<c:out value='${parameter.key}' />" value="<c:out value='${parameter.value}' />">
              </c:forEach>
              <label>Go to page
                <input type="number" name="page" min="1" max="${view.totalPages}" value="${view.currentPage}" required>
              </label>
              <button type="submit">Go</button>
            </form>
          </nav>
        </c:if>
      </c:otherwise>
    </c:choose>
  </main>
</div>
<%@ include file="site-footer.jspf" %>
<script src="<c:out value='${assets}' />/app.js" defer></script>
</body>
</html>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="en-US" scope="request" />
<c:set var="assets" value="${pageContext.request.contextPath}/assets" />
<c:set var="paperPath" value="${pageContext.request.contextPath}/paper.jsp" />
<c:set var="proceedingsPath" value="${basePath}" />
<c:set var="suggestPath" value="${pageContext.request.contextPath}/suggest.json" />
<c:set var="venueNames" value="${view.venueNames}" />
<c:set var="currentPage" value="proceedings" />
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
    <form class="filter-form proceedings-form" method="get" action="<c:out value='${basePath}' />">
      <label class="field">
        <span class="visually-hidden">Conference</span>
        <select name="venue" aria-label="Conference">
          <option value="">Conference…</option>
          <c:forEach var="category" items="${view.categories}">
            <optgroup label="<c:out value='${category.name}' />">
              <c:forEach var="conference" items="${category.conferences}">
                <option value="<c:out value='${conference.slug}' />"${conference.slug == view.venue ? ' selected' : ''}><c:out value="${conference.name}" /></option>
              </c:forEach>
            </optgroup>
          </c:forEach>
        </select>
      </label>
      <label class="field">
        <span class="visually-hidden">Year</span>
        <select name="year" aria-label="Year">
          <c:choose>
            <c:when test="${not empty view.venueYears}">
              <c:forEach var="entry" items="${view.venueYears}">
                <option value="${entry.year}"${entry.selected ? ' selected' : ''}>${entry.year}<c:if test="${not empty entry.mark}"> (<c:out value="${entry.mark}" />)</c:if></option>
              </c:forEach>
            </c:when>
            <c:otherwise>
              <option value="">Latest year</option>
            </c:otherwise>
          </c:choose>
        </select>
      </label>
      <button type="submit" class="button">Open</button>
    </form>
  </div>
  <div class="topbar-right">
    <c:set var="searchQuery" value="" />
    <c:set var="searchHidden" value="${null}" />
    <%@ include file="site-search.jspf" %>
    <%@ include file="site-nav.jspf" %>
  </div>
</header>

<main id="papers" class="content content-wide">
  <c:choose>
    <c:when test="${view.showingOverview}">
      <div class="results-header">
        <h1 class="results-heading">Proceedings</h1>
        <p class="results-count">Pick a conference and year to read its program front to back.</p>
      </div>
      <div class="proceedings-overview">
        <c:forEach var="category" items="${view.overview}">
          <section class="overview-category">
            <h2 class="overview-category-name"><c:out value="${category.name}" /></h2>
            <ul class="overview-venues">
              <c:forEach var="venue" items="${category.venues}">
                <li class="overview-venue">
                  <span class="overview-venue-name" title="<c:out value='${venue.fullName}' />"><c:out value="${venue.name}" /></span>
                  <span class="overview-years">
                    <c:forEach var="entry" items="${venue.years}">
                      <a href="<c:out value='${basePath}' />?venue=<c:out value='${venue.slug}' />&amp;year=${entry.year}"
                         <c:if test="${not empty entry.mark}">title="<c:out value='${entry.coverage.label}' />"</c:if>>
                        ${entry.year}<c:if test="${entry.showCount}"> <span class="count"><fmt:formatNumber value="${entry.count}" /></span></c:if><c:if test="${not empty entry.mark}"> <span class="coverage-mark"><c:out value="${entry.mark}" /></span></c:if>
                      </a>
                    </c:forEach>
                  </span>
                </li>
              </c:forEach>
            </ul>
          </section>
        </c:forEach>
      </div>
    </c:when>

    <c:otherwise>
      <div class="results-header">
        <h1 class="results-heading"><c:out value="${view.heading}" /></h1>
        <p class="results-count">
          <c:out value="${view.venueFullName}" />
          <c:if test="${view.totalPapers > 0}"> · <fmt:formatNumber value="${view.totalPapers}" /> papers</c:if>
        </p>
        <a class="results-proceedings" href="<c:out value='${view.searchUrl}' />">Search within this program &rarr;</a>
      </div>

      <c:if test="${view.coverage != null and not view.coverage.complete}">
        <p class="notice notice-coverage" role="status">
          Coverage for <c:out value="${view.heading}" />: <strong><c:out value="${view.coverage.label}" /></strong>.
          <c:if test="${not empty view.coverage.lastVerifiedDate}">Last verified <c:out value="${view.coverage.lastVerifiedDate}" />.</c:if>
        </p>
      </c:if>

      <c:if test="${not empty view.venueYears}">
        <p class="year-switcher">
          <c:forEach var="entry" items="${view.venueYears}">
            <a href="<c:out value='${entry.href}' />"<c:if test="${entry.selected}"> aria-current="page"</c:if>>${entry.year}</a>
          </c:forEach>
        </p>
      </c:if>

      <c:choose>
        <c:when test="${view.noProgram}">
          <div class="empty-state">
            <p><c:out value="${view.heading}" />: <c:out value="${view.coverage.label}" />, so there is no program to read yet.</p>
          </div>
        </c:when>
        <c:when test="${empty view.sessions}">
          <div class="empty-state"><p>No papers have been collected for <c:out value="${view.heading}" />.</p></div>
        </c:when>
        <c:otherwise>
          <c:if test="${view.hasSessions}">
            <nav class="session-toc" aria-label="Sessions">
              <h2 class="session-toc-title">Sessions</h2>
              <ol class="session-toc-list">
                <c:forEach var="session" items="${view.sessions}">
                  <li><a href="#<c:out value='${session.anchor}' />"><c:out value="${empty session.name ? 'Papers without a session' : session.name}" /></a> <span class="count">${session.count}</span></li>
                </c:forEach>
              </ol>
            </nav>
          </c:if>

          <c:set var="showVenue" value="${false}" />
          <c:forEach var="session" items="${view.sessions}">
            <section class="session" id="<c:out value='${session.anchor}' />">
              <c:if test="${view.hasSessions}">
                <h2 class="session-name"><c:out value="${empty session.name ? 'Papers without a session' : session.name}" /> <span class="count">${session.count}</span></h2>
              </c:if>
              <ol class="paper-list">
                <c:forEach var="paper" items="${session.papers}">
                  <%@ include file="paper-entry.jspf" %>
                </c:forEach>
              </ol>
            </section>
          </c:forEach>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>
</main>
<%@ include file="site-footer.jspf" %>
<script src="<c:out value='${assets}' />/app.js" defer></script>
</body>
</html>

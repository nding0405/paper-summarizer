<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="en-US" scope="request" />
<c:set var="assets" value="${pageContext.request.contextPath}/assets" />
<c:set var="paper" value="${view.paper}" />
<c:set var="paperPath" value="" />
<c:set var="currentPage" value="paper" />
<c:set var="suggestPath" value="${pageContext.request.contextPath}/suggest.json" />
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><c:out value="${paper.title}" /></title>
<link rel="stylesheet" href="<c:out value='${assets}' />/app.css">
</head>
<body>
<header class="topbar">
  <div class="topbar-left">
    <%@ include file="site-home.jspf" %>
    <p class="breadcrumbs">
      <a href="<c:out value='${view.proceedingsUrl}' />"><c:out value="${view.venueName} ${paper.year}" /> proceedings</a>
      <span class="muted">·</span>
      <a href="<c:out value='${view.searchUrl}' />">Search this conference-year</a>
    </p>
  </div>
  <div class="topbar-right">
    <c:set var="searchQuery" value="" />
    <c:set var="searchHidden" value="${null}" />
    <%@ include file="site-search.jspf" %>
    <%@ include file="site-nav.jspf" %>
  </div>
</header>

<main class="content paper-page">
  <article class="paper paper-full">
    <h1 class="paper-page-title"><c:out value="${paper.title}" /></h1>
    <p class="paper-authors-full">
      <c:forEach var="author" items="${paper.authors}" varStatus="loop"><c:out value="${author}" /><c:if test="${not loop.last}">, </c:if></c:forEach>
    </p>
    <c:if test="${paper.hasInstitutions}">
      <p class="paper-institutions"><c:out value="${paper.institutionLine}" /></p>
    </c:if>
    <p class="paper-meta">
      <span class="paper-venue"><c:out value="${view.venueName} ${paper.year}" /></span>
      <c:if test="${not empty paper.track}"><span class="muted"><c:out value="${paper.track}" /></span></c:if>
      <c:if test="${paper.showRelevanceTag}"><span class="tag tag-relevance"><c:out value="${paper.relevanceLabel}" /></span></c:if>
      <c:forEach var="topic" items="${paper.topicLabels}"><span class="tag"><c:out value="${topic}" /></span></c:forEach>
    </p>
    <c:if test="${paper.hasTakeaway}">
      <p class="paper-takeaway"><c:out value="${paper.takeaway}" /></p>
    </c:if>
    <c:if test="${paper.hasLinks}">
      <%@ include file="paper-links.jspf" %>
    </c:if>

    <div class="paper-body paper-body-full">
      <%@ include file="paper-body.jspf" %>
    </div>
  </article>
</main>
<%@ include file="site-footer.jspf" %>
<script src="<c:out value='${assets}' />/app.js" defer></script>
</body>
</html>

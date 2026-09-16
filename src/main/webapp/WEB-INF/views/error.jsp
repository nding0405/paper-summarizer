<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" isErrorPage="true" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="status" value="${pageContext.errorData.statusCode}" />
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Error <c:out value="${status}" /></title>
<link rel="stylesheet" href="<c:out value='${pageContext.request.contextPath}' />/assets/app.css">
</head>
<body>
<main class="content error-page">
  <h1 class="results-heading">
    <c:choose>
      <c:when test="${status == 404}">Page not found</c:when>
      <c:otherwise>Something went wrong</c:otherwise>
    </c:choose>
  </h1>
  <p>
    <c:choose>
      <c:when test="${status == 404}">There is only one page: the paper list.</c:when>
      <c:otherwise>The server could not render this page (HTTP <c:out value="${status}" />). Check the Tomcat log for details.</c:otherwise>
    </c:choose>
  </p>
  <p><a href="<c:out value='${pageContext.request.contextPath}' />/index.jsp">Back to the paper list</a></p>
</main>
</body>
</html>

<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="keys" class="ru.skbkontur.octopnp.CommonConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    Octopus URL:
    <strong><props:displayValue name="${keys.octopusServerUrlKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Package Version:
    <strong><props:displayValue name="${keys.packageVersionKey}" emptyValue="not specified"/></strong>
</div>

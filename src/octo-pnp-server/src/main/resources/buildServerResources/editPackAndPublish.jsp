<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="keys" class="ru.skbkontur.octopnp.CommonConstants" />
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Octopus Connection">
<tr>
  <th>Octopus server URL:<l:star/></th>
  <td>
    <props:textProperty name="${keys.octopusServerUrlKey}" className="longField"/>
    <span class="error" id="error_${keys.octopusServerUrlKey}"></span>
    <span class="smallNote">Specify Octopus web portal URL (e.g. https://octo.skbkontur.ru)</span>
  </td>
</tr>
<tr>
  <th>Octopus API key:<l:star/></th>
  <td>
    <props:passwordProperty name="${keys.octopusApiKey}" className="longField"/>
    <span class="error" id="error_${keys.octopusApiKey}"></span>
    <span class="smallNote">Specify Octopus API key. You can get this from your user page in the Octopus web portal.</span>
  </td>
</tr>
</l:settingsGroup>

<l:settingsGroup title="Packages">
  <tr>
    <th>.nuspec file paths:<l:star/></th>
    <td>
      <props:multilineProperty name="${keys.nuspecPathsKey}" rows="5" cols="50" linkTitle="Nuspec path patterns" expanded="true" />
      <span class="error" id="error_${keys.nuspecPathsKey}"></span>
      <span class="smallNote">
        Newline-separated paths of .nuspec files to create packages from, that will be published, e.g. MyProject/**/octopus-package.nuspec.
      </span>
    </td>
  </tr>
  <tr>
      <th>Package Version:</th>
      <td>
        <props:textProperty name="${keys.packageVersionKey}" className="longField"/>
        <span class="error" id="error_${keys.packageVersionKey}"></span>
        <span class="smallNote">The number to use for this release, e.g., <code>1.0.%build.number%</code>. Overrides the version number from the .nuspec file.</span>
      </td>
   </tr>
   <tr>
     <th>Push Concurrency:</th>
     <td>
       <props:textProperty name="${keys.pushConcurrencyKey}"/>
       <span class="smallNote">The number of push threads. If less or equal 0 will push packages in parallel.</span>
      </td>
   </tr>
</l:settingsGroup>

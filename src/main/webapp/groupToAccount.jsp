<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="gov.bnl.gums.*"%>
<jsp:useBean id="gums" scope="application" class="gov.bnl.gums.admin.GUMSAPIImpl" />
<%@ page import="gov.bnl.gums.account.*" %>
<%@ page import="gov.bnl.gums.groupToAccount.*" %>
<%@ page import="gov.bnl.gums.userGroup.*" %>
<%@ page import="gov.bnl.gums.configuration.*" %>
<%@ page import="gov.bnl.gums.service.ConfigurationWebToolkit" %>
<%@ page import="java.util.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
 	<title>GUMS</title>
 	<link href="gums.css" type="text/css" rel="stylesheet"/>
</head>
<body>
<%@include file="topNav.jspf"%>
<div id="title">
<h1><span>GUMS</span></h1>
<h2><span>Group To Account</span></h2>
</div>
<%@include file="sideNav.jspf"%>
<div id="body">
<%-- <jsp:useBean id="beanInstanceName" scope="session" class="beanPackage.BeanClassName" /> --%>
<%-- <jsp:getProperty name="beanInstanceName"  property="propertyName" /> --%>

<%
Configuration configuration = null;
try {
	configuration = gums.getConfiguration();
}catch(Exception e){
%>

<p><div class="failure">Error getting configuration: <%= e.getMessage() %></div></p>
</div>
<%@include file="bottomNav.jspf"%>
</body>
</html>

<%
	return;
}
if (configuration.getUserGroups().size()==0) {
%>
	<p><div class="failure">You must first create a user group.</div></p>
	</div>
	<%@include file="bottomNav.jspf"%>
	</body>
	</html>
<%
	return;
}
if (configuration.getAccountMappers().size()==0) {
%>
	<p><div class="failure">You must first create an account mapper.</div></p>
	</div>
	<%@include file="bottomNav.jspf"%>
	</body>
	</html>
<%
	return;
}
%>

<p>
Configures group to account mappings.
</p>

<%
String message = null;

if (request.getParameter("action")==null || 
	"save".equals(request.getParameter("action")) || 
	"delete".equals(request.getParameter("action"))) {
	
	if ("save".equals(request.getParameter("action"))) {
		Configuration newConfiguration = (Configuration)configuration.clone();
		try{
			newConfiguration.removeGroupToAccountMapping( request.getParameter("name") );
			newConfiguration.addGroupToAccountMapping( ConfigurationWebToolkit.parseGroupToAccountMapping(request) );
			gums.setConfiguration(newConfiguration);
			configuration = gums.getConfiguration();
			message = "<div class=\"success\">Group to account mapping has been saved.</div>";
		}catch(Exception e){
			message = "<div class=\"failure\">Error saving group to account mapping: " + e.getMessage() + "</div>";
		}
	}

	if ("delete".equals(request.getParameter("action"))) {
		Configuration newConfiguration = (Configuration)configuration.clone();
		try{
			String references = ConfigurationWebToolkit.getHostToGroupReferences(newConfiguration, request.getParameter("name"));
			if( references==null ) {
				if (newConfiguration.removeGroupToAccountMapping( request.getParameter("name") )!=null) {
					gums.setConfiguration(newConfiguration);
					configuration = gums.getConfiguration();
					message = "<div class=\"success\">Group to account mapping has been deleted.</div>";
				}
				else
					message = "<div class=\"failure\">Error deleting group to account mapping</div>";
			}
			else
				message = "<div class=\"failure\">You cannot delete this item until removing references to it within host to group mapping(s) that match against " + references + "</div>";
		}catch(Exception e){
			message = "<div class=\"failure\">Error deleting group to account mapping: " + e.getMessage() + "</div>";
		}
	}

	Collection g2AMappings = configuration.getGroupToAccountMappings().values();

	out.write(
"<table id=\"form\" cellpadding=\"2\" cellspacing=\"2\">");

	if(message!=null)
		out.write( "<tr><td colspan=\"2\">" + message + "</td></tr>" );
				
	Iterator g2AMappingsIt = g2AMappings.iterator();
	while(g2AMappingsIt.hasNext()) {
		GroupToAccountMapping g2AMapping = g2AMappingsIt.hasNext() ? (GroupToAccountMapping)g2AMappingsIt.next() : null;
		
%>	
	   	<tr>
			<td width="55" valign="top">
				<form action="groupToAccount.jsp" method="get">
					<input type="image" src="images/Edit24.gif" name="action" value="edit">
					<input type="image" src="images/Remove24.gif" name="action" value="delete" onclick="if(!confirm('Are you sure you want to delete this group to account mapping?'))return false;">
					<input type="hidden" name="name" value="<%=g2AMapping.getName()%>">
				</form>
			</td>
	  		<td align=left>
		   		<table class="configElement" width="100%">
		  			<tr>
			    		<td>
							group to account mapper:
							<a href="groupToAccount.jsp?action=edit&name=<%=g2AMapping.getName()%>">
								<%=g2AMapping.getName()%>
							</a><br>
							description: <%=g2AMapping.getDescription()%><br>		    		
<%				    		
		out.write(			"user group" + (g2AMapping.getUserGroups().size()>1 ? "s: " : ": ") );
		
		Iterator userGroupsIt = g2AMapping.getUserGroups().iterator();
		while(userGroupsIt.hasNext())
		{
			String userGroup = (String)userGroupsIt.next();
			out.write( "<a href=\"userGroups.jsp?action=edit&name=" + userGroup + "\">" + userGroup + "</a>" );
			if( userGroupsIt.hasNext() )
				out.write(", ");
		}
%>
		<br>
<%
		out.write(			"account mapper" + (g2AMapping.getAccountMappers().size()>1 ? "s: " : ": ") );
		
		Iterator accountMappersIt = g2AMapping.getAccountMappers().iterator();
		while(accountMappersIt.hasNext())
		{
			String accountMapper = (String)accountMappersIt.next();
			out.write( "<a href=\"accountMappers.jsp?action=edit&name=" + accountMapper + "\">" + accountMapper + "</a>" );
			if( accountMappersIt.hasNext() )
				out.write(", ");
		}
%>	
						</td>
			      	</tr>
				</table>
			</td>
			<td width=\"10\"></td>		
		</tr>
<%
	}
%>
		<tr>
	        <td colspan=2>
	        	<form action="groupToAccount.jsp" method="get">
	        		<div style="text-align: center;"><button type="submit" name="action" value="add">Add</button></div>
	        	</form>
	        </td>
		</tr>
	 </table>
</form>
<%
}

else if ("edit".equals(request.getParameter("action"))
	|| "add".equals(request.getParameter("action"))
	|| "reload".equals(request.getParameter("action"))) {
	
	Collection g2AMappings = configuration.getGroupToAccountMappings().values();
	
	GroupToAccountMapping g2AMapping = null;
	
	if ("edit".equals(request.getParameter("action"))) {
		try {
			g2AMapping = (GroupToAccountMapping)configuration.getGroupToAccountMappings().get( request.getParameter("name") );
		} catch(Exception e) {
			out.write( "<div class=\"failure\">Error getting group to account mapping: " + e.getMessage() + "</div>" );
			return;
		}
	}

	if ("reload".equals(request.getParameter("action"))) {
		try{
			g2AMapping = ConfigurationWebToolkit.parseGroupToAccountMapping(request);
		} catch(Exception e) {
			out.write( "<div class=\"failure\">Error reloading group to account mapping: " + e.getMessage() + "</div>" );
			return;
		}
	}
		
	else if ("add".equals(request.getParameter("action"))) {
		g2AMapping = new GroupToAccountMapping(configuration);
	}
%>

<form action="groupToAccount.jsp" method="get">
	<input type="hidden" name="action" value="">
	<input type="hidden" name="originalAction" value="<%=("reload".equals(request.getParameter("action")) ? request.getParameter("originalAction") : request.getParameter("action"))%>">
	<table id="form" border="0" cellpadding="2" cellspacing="2" align="center">
		<tr>
    		<td nowrap style="text-align: right;">
	    		For requests routed to group
		    </td>
		    <td nowrap>
<%

	if ("add".equals(request.getParameter("action")) || "add".equals(request.getParameter("originalAction"))) {
%>
		    	<input maxlength="256" size="32" name="name" value="<%=(g2AMapping.getName()!=null ? g2AMapping.getName() : "")%>"/>
		    </td>
		</tr>
		<tr>
			<td nowrap style="text-align: right;">
	    		i.e.
		    </td>
		    <td nowrap>
				myGroupToAccountMapping
		    </td>
		</tr>
<%
	}
	else {
%>
		    	<%=g2AMapping.getName()%>
		    	<input type="hidden" name="name" value="<%=g2AMapping.getName()%>"/>
		   </td>
		</tr>
<%
	}
%>
		<tr>
			<td nowrap style="text-align: right;">
	    		with description:
		    </td>
		    <td nowrap>
				<input name="description" size="64" value="<%=g2AMapping.getDescription()%>"/>
		    </td>
		</tr>	
		<tr>
			<td nowrap style="text-align: right;">where user member of user group</td>
			<td>
<%
	
	// Create multiple user groups
	int counter = 0;
	if (g2AMapping!=null) {
		Collection userGroups = g2AMapping.getUserGroups();
		Iterator userGroupsIt = userGroups.iterator();
		while(userGroupsIt.hasNext())
		{
			out.write( 
				ConfigurationWebToolkit.createSelectBox("uG"+counter, 
					configuration.getUserGroups().values(), 
					(String)userGroupsIt.next(),
					"onchange=\"document.forms[0].elements['action'].value='reload';document.forms[0].submit();\"",
					counter!=0) );
			counter++;
		}
	}
	out.write( 
		ConfigurationWebToolkit.createSelectBox("uG"+counter, 
			configuration.getUserGroups().values(), 
			null,
			"onchange=\"document.forms[0].elements['action'].value='reload';document.forms[0].submit();\"",
			counter!=0)+
			"(try in order)");
%>
		    </td>
		</tr>
		<tr>
			<td nowrap style="text-align: right;">route request to account mapper(s)</td>
			<td>
<%	
	// Create multiple group to account mappings
	counter = 0;
	if (g2AMapping!=null) {
		Collection accountMappers = g2AMapping.getAccountMappers();
		Iterator accountMappersIt = accountMappers.iterator();
		while(accountMappersIt.hasNext())
		{
			out.write( 
				ConfigurationWebToolkit.createSelectBox("aM"+counter, 
					configuration.getAccountMappers().values(), 
					(String)accountMappersIt.next(),
					"onchange=\"document.forms[0].elements['action'].value='reload';document.forms[0].submit();\"",
					true) );
			counter++;
		}
	}
	out.write( 
		ConfigurationWebToolkit.createSelectBox("aM"+counter, 
			configuration.getAccountMappers().values(), 
			null,
			"onchange=\"document.forms[0].elements['action'].value='reload';document.forms[0].submit();\"",
			counter!=0)+
			"(try in order) .");
%>
			</td>
		</tr>
		<tr>
    		<td nowrap style="text-align: right;">Accounting VO (optional for Grid3-User-VO-Map) </td>
		    <td nowrap><input maxlength="256" size="32" name="vo" value="<%=g2AMapping.getAccountingVo()%>"/></td>
		</tr>
		<tr>
    		<td nowrap style="text-align: right;">Accounting Description (optional for Grid3-User-VO-Map) </td>
		    <td nowrap><input maxlength="256" size="64" name="desc" value="<%=g2AMapping.getAccountingDesc()%>"/></td>
		</tr>
		<tr>
	        <td colspan=2>
	        	<div style="text-align: center;">
				<%=ConfigurationWebToolkit.createDoSubmit(g2AMappings, request)%>
	        	<div style="text-align: center;">
	        		<button type="submit" onclick="return doSubmit()">Save</button>
	        	</div>
	        	</div>
	        </td>
		</tr>
	</table>
</form>
<%
}
%>

</div>
<%@include file="bottomNav.jspf"%>
</body>
</html>

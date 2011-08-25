<%@ page import="com.tropo.web.*,com.tropo.server.*,com.tropo.server.jmx.*" %>
<%@page import="org.springframework.web.context.WebApplicationContext"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
		
<%
	if (getServletConfig().getServletContext().getAttribute(ContextLoaderListener.TROPO_STATUS) == TropoStatus.FAILED) {
	    response.sendError(500);
    }
    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(application);        
	AdminService adminService = (AdminService)context.getBean("adminService");
	Calls calls = (Calls)context.getBean("callsBean");
	RayoStatistics rayoStatistics = (RayoStatistics)context.getBean("rayoStatistics");
	Info info = (Info)context.getBean("infoBean");
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"> 
<html> 
<head> 
	<title>Rayo Server - Dashboard</title>
	<link href="css/rayo.css" rel="stylesheet" type="text/css">  
	<link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.10/themes/start/jquery-ui.css"/>
</head> 
<body class="home"> 
  <div id="header">
    <div id="header-content">
      <div id="logo"></div>
    </div>
  </div>
  <div id="main">
	<div class="dashboard">
	  <div class="dashboard-content">
	    <div class="dashboard-info">
	    	<div class="dashboard-info-header">
	    		Server Status
	    	</div>
	    	<div class="dashboard-info-detail">
	    		<ul>
	    			<li><strong>Build Number:</strong> <%= adminService.getBuildNumber() %></li>
	    			<li><strong>Uptime:</strong> <%= info.getUptime() %></li>
	    			<li><strong>Total Calls:</strong> <%= calls.getTotalCalls() %></li>
	    			<li><strong>Commands:</strong> <%= rayoStatistics.getTotalCommands() %></li>
	    			<li><strong>Events:</strong> <%= rayoStatistics.getCallEventsProcessed() %></li>
	    		</ul>
	    	</div>
		</div>
		<div class="dashboard-congrats">
	      <div class="heading">Congratulations!</div>
	      <div class="heading">Your Rayo Server is online.</div>
		  <div id="banner-icon"></div>
		</div>
	  </div>
	</div>
	<div class="portlet ui-widget-content ui-helper-clearfix ui-corner-all" id="portlet-template">
	  <div class="portlet-header ui-widget-header ui-corner-all"><span class='ui-icon ui-icon-minusthick'></span><span class="title">&nbsp;</span></div>
	  <div class="portlet-content"></div>
	</div>

	<div class="column"></div>
	
  </div>  

  
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.10/jquery-ui.js"></script>
  <script src="js/jolokia.js"></script>
  <script src="js/jolokia-simple.js"></script>
  <script src="js/highcharts.src.js"></script>
  <script src="js/dashboard.js"></script>
  
</body> 
</html> 

 
 
 
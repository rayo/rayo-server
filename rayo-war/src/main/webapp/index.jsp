<%@ page import="com.rayo.web.*,com.rayo.server.*,com.rayo.server.web.*,com.rayo.server.admin.*,com.rayo.server.jmx.*" %>
<%@page import="org.springframework.web.context.WebApplicationContext"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
		
<%
	if (getServletConfig().getServletContext().getAttribute(ContextLoaderListener.RAYO_STATUS) == RayoStatus.FAILED) {
	    response.sendError(500);
    }
    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(application);        
	AdminService adminService = (AdminService)context.getBean("adminService");
	Calls calls = (Calls)context.getBean("callsBean");
	RayoStatistics rayoStatistics = (RayoStatistics)context.getBean("rayoStatistics");
	MixerStatistics mixerStatistics = (MixerStatistics)context.getBean("mixerStatistics");
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
	    			<li><strong>Uptime:</strong> <span id="uptime"><%= info.getUptime() %></span></li>
	    			<li><strong>Total Calls:</strong> <span id="calls"><%= calls.getTotalCalls() %></span></li>
	    			<li><strong>Total Conferences:</strong> <span id="conferences"><%= mixerStatistics.getTotalMixers() %></span></li>
	    			<li><strong>Commands:</strong> <span id="commands"><%= rayoStatistics.getTotalCommands() %></span></li>
	    			<li><strong>Events:</strong> <span id="events"><%= rayoStatistics.getCallEventsProcessed() %></span></li>
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
  <script src="js/periodic.js"></script>
  <script>
	  $(document).ready(function() {
		$.periodic({period: 1000}, function() {
			var j4p = new Jolokia("<%=request.getContextPath()%>/jmx");	
			var uptime = { type: "read", mbean: "com.rayo:Type=Info", attribute: "Uptime" };
			var calls = { type: "read", mbean: "com.rayo:Type=Calls", attribute: "TotalCalls" };
			var conferences = { type: "read", mbean: "com.rayo:Type=Mixer Statistics", attribute: "TotalMixers" };
			var events = { type: "read", mbean: "com.rayo:Type=Rayo", attribute: "CallEventsProcessed" };
			var commands = { type: "read", mbean: "com.rayo:Type=Rayo", attribute: "TotalCommands" };
			var responses = j4p.request([uptime,calls,conferences,events,commands]);
			if (responses[0] != null) {
		    	$("#uptime").text(responses[0].value);
			}
			if (responses[1] != null) {
		    	$("#calls").text(responses[1].value);
			}
			if (responses[2] != null) {
		    	$("#conferences").text(responses[2].value);
			}
			if (responses[3] != null) {
		    	$("#events").text(responses[3].value);
			}
			if (responses[4] != null) {
		    	$("#commands").text(responses[4].value);
			}
		});	
	  });  
  
  </script>
</body> 
</html>

 
 
 
<%@page import="com.rayo.server.listener.XmppMessageListenerGroup"%>
<%@page import="com.rayo.web.*,com.rayo.server.*,com.rayo.web.websockets.*" %>
<%@page import="org.springframework.web.context.WebApplicationContext"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
		
<%
	DebugXmppMessageListener listener = (DebugXmppMessageListener)session
    	.getAttribute(RayoServletSessionListener.XMPP_LISTENER);
	if (listener == null) {
	    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(application);   
	    XmppMessageListenerGroup group = (XmppMessageListenerGroup)context.getBean("xmppMessageListenersGroup");
		listener = new DebugXmppMessageListener();
		session.setAttribute(RayoServletSessionListener.XMPP_LISTENER, listener);
		group.addXmppMessageListener(listener);
	}
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"> 
<html> 
<head> 
	<title>Rayo Server - Debug Console</title>
	<link href="css/rayo.css" rel="stylesheet" type="text/css">  
	<link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.10/themes/start/jquery-ui.css"/>
	<script type="text/javascript" src="js/jquery-1.5.1.min.js"></script>
	<script type="text/javascript" src="js/jquery-ui-1.8.11.custom.min.js"></script>
</head> 

<body class="home" onload='openWebSocket()' onunload='closeWebSocket()'> 
	<script language="javascript" type="text/javascript"> 
	    var socket;
	    
	    function openWebSocket() {
	      if (window.WebSocket) {
	        socket = new WebSocket('ws://<%=request.getServerName()%>:10000/websocket');
	        socket.onopen = function(event) {
	          toggle(true);
	        };
	        socket.onclose = function(event) {
	          toggle(false);
	        };
	        socket.onmessage = function(event) {
	          onMessage(event.data);
	        };
	      } else {
	        alert('Your browser does not support WebSockets yet.');
	      }
	    }
	    
	    function closeWebSocket() {
	    	socket.close();
	    }
	    
	    function send(message) {
	        
	    	if (!window.WebSocket) {
	          return;
	        }
	        if (socket.readyState == WebSocket.OPEN) {
	          socket.send(message);
	        } else {
	          alert('The WebSocket is not open!');
	        }
	    }
	    
	    function onMessage(response) {
	    	 
	        var data = response;
	        //$('#messages').html(data);
	        //$('#table').html('<tr><td>'+data+'</td></tr>');
	        $("#table tbody").prepend("<tr><td>"+data+"</td></tr>");
	    }
	    
	    function toggle(flag) {

	        if (flag) {
	        	send('')
	        } else {

	        }
	    }
	</script>

  <div id="header">
    <div id="header-content">
      <div id="logo"></div>
    </div>
  </div>
  <div id="main-debug">
	<div class="portlet-big ui-widget-content ui-helper-clearfix ui-corner-all">
	  <div class="portlet-header ui-widget-header ui-corner-all"><span class='ui-icon ui-icon-minusthick'></span><span class="title">XMPP Messages</span></div>
	  <div class="portlet-content-big">
		<table id="table">
			<tbody id="tablebody">
				<tr><div id="messages"></div></tr>						
			</tbody>
		</table>	
	  </div>
	</div>
  
  </div>  
</body> 
</html>

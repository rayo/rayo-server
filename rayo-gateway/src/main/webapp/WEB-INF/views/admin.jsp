<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Bootstrap, from Twitter</title>
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le HTML5 shim, for IE6-8 support of HTML elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <!-- Le styles -->
    <link rel="stylesheet" href="http://twitter.github.com/bootstrap/1.4.0/bootstrap.min.css">
    <link href="css/admin.css" rel="stylesheet" type="text/css"> 
    
    <style type="text/css">
      body {
        padding-top: 60px;
      }
    </style>

    <!-- Le fav and touch icons -->
    <link rel="shortcut icon" href="images/favicon.ico">

    <link rel="apple-touch-icon" href="images/apple-touch-icon.png">
    <link rel="apple-touch-icon" sizes="72x72" href="images/apple-touch-icon-72x72.png">

    <link rel="apple-touch-icon" sizes="114x114" href="images/apple-touch-icon-114x114.png">
  </head>

  <body>

    <div class="topbar">
      <div class="topbar-inner">

        <div class="container-fluid">
          <a class="brand" href="dashboard/index">Rayo Dashboard</a>

          <ul class="nav">
            <li class="active"><a href="#">Home</a></li>
            <li><a href="http://www.rayo.org">About</a></li>
            <li><a href="http://voxeolabs.com/about/contact/">Contact</a></li>

          </ul>
          <div id="logo"></div>
        </div>
        
      </div>
    </div>

    <div class="container-fluid">
      <div class="sidebar">

        <div class="well">
          <h5>Documentation</h5>
          <ul>
            <li><a href="https://github.com/rayo/rayo-server/wiki">Wiki</a></li>
          </ul>
          <h5>Project</h5>

          <ul>
            <li><a href="http://github.com/rayo">Source Code</a></li>
            <li><a href="http://ci.voxeolabs.net/view/Rayo">CI Server</a></li>
            <li><a href="http://github.com/rayo/xmpp">Specification</a></li>
          </ul>
          <h5>Client Libraries</h5>

          <ul>
            <li><a href="http://www.adhearsion.com">Ruby</a></li>
            <li><a href="http://www.github.com/voxeolabs/moho">Java</a></li>
            <li><a href="http://www.github.com/rayo/node-rayo">Node</a></li>
          </ul>

        </div>
      </div>    
      <div class="content">

		<ul class="breadcrumb">
		  <li><a href="admin.jsp">Gateway</a></li>
		</ul>
		<div class="row">
		  <div class="span6">
			<div class="information"></div>
		  </div>
		  <div class="span6">
			<div class="server"></div>
		  </div>
		  <div class="span6">
		  	<h2>Heap Usage</h2>
			<div class="portlet ui-widget-content ui-helper-clearfix ui-corner-all" id="portlet-template">
			  <div class="portlet-header ui-widget-header ui-corner-all"><span class='ui-icon ui-icon-minusthick'></span><span class="title">&nbsp;</span></div>
			  <div class="portlet-content"></div>
			</div>
			<div class="column"></div>
		  </div>
		</div>
        <hr>

        <div class="row">
          <div class="span6">
			<div class="platforms"></div>
          </div>
          <div class="span6">
            <div class="nodes"></div>
          </div>
          <div class="span6">
            <div class="clients"></div>
          </div>
        </div>
        
        <hr>
        
        <div class="row">
          <div class="span6">
			<div class="applications"></div>
          </div>
        </div>
        <footer>
          <p>&copy; VoxeoLabs 2012</p>
        </footer>
      </div>
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
		var table = new JmxTable();
		table.create('../jmx', 'Information',
					[{ type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "ActiveRayoNodesCount"},
					 { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "TotalCallsCount"},
					 { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "ActiveCallsCount"},
					 { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "TotalClientsCount"},
					 { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "ActiveClientsCount"},
					 { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "MessagesCount"},
					 { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "ErrorsCount"}], 
					['Rayo Nodes', 'Total Calls', 'Active Calls', 'Total Clients', 'Active Clients', 'Total Messages', 'Total Errors'], 
					'.information',
					5000);
					
		table.create('../jmx', 'Rayo Server',
					[{ type: "read", mbean: "com.rayo:Type=Info", attribute: "Uptime"},
					 { type: "read", mbean: "com.rayo:Type=Info", attribute: "BuildNumber"},
					 { type: "read", mbean: "com.rayo:Type=Info", attribute: "BuildId"},
					 { type: "read", mbean: "com.rayo.gateway:Type=Admin,name=Admin", attribute: "QuiesceMode"},
					 { type: "read", mbean: "java.lang:type=OperatingSystem", attribute: "SystemLoadAverage"},
					 { type: "read", mbean: "java.lang:type=OperatingSystem", attribute: "FreePhysicalMemorySize"},
					 { type: "read", mbean: "java.lang:type=Threading", attribute: "ThreadCount"}],
					['Uptime', 'Build Number', 'Build Id', 'Quiesced', 'System Load Average', 'Free Memory', 'Thread Count'], 
					'.server',
					5000);					

		table.createFromList('../jmx', 'Platforms',
					{ type: "read", mbean: "com.rayo.gateway:Type=Gateway", attribute: "Platforms"}, 
					"name",
					"./platforms",
					'.platforms');
					
             table.createFromList('../jmx', 'Nodes',
                                        { type: "read", mbean: "com.rayo.gateway:Type=Gateway", attribute: "RayoNodes"},
					"hostname",
                                        "./nodes",
                                        '.nodes');

             table.showList('../jmx', 
             				'Active Clients',
                            { type: "read", mbean: "com.rayo.gateway:Type=Gateway", attribute: "ActiveClients"},
                            '.clients');
                                                                                
             table.createFromList('../jmx', 'Applications',
                                        { type: "read", mbean: "com.rayo.gateway:Type=Gateway", attribute: "ClientApplications"},
                                        "appId",
                                        "./applications",
                                        '.applications');
                                        
			var factory = new JmxChartsFactory('../jmx');
			factory.create([
				{
					name: 'java.lang:type=Memory',
					attribute: 'HeapMemoryUsage',
					path: 'committed'
				},
				{
					name: 'java.lang:type=Memory',
					attribute: 'HeapMemoryUsage',
					path: 'used'
				}
			]);                                        
        });
	
  </script>
  </body>
</html>


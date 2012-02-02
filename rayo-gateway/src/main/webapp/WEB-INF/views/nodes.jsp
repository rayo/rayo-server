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
          <a class="brand" href="#">Rayo Dashboard</a>

          <ul class="nav">
            <li class="active"><a href="#">Home</a></li>
            <li><a href="#about">About</a></li>
            <li><a href="#contact">Contact</a></li>

          </ul>
          <p class="pull-right">Logged in as <a href="#">username</a></p>
        </div>
      </div>
    </div>

    <div class="container-fluid">
      <div class="content">

		<ul class="breadcrumb">
		  <li><a href="../index">Gateway</a> / <a href=".">${node}</a></li>
		</ul>
		<div class="row">
		  <div class="span6">
			<div class="information"></div>
		  </div>
		</div>
        <hr>

        <footer>
          <p>&copy; VoxeoLabs 2012</p>
        </footer>
      </div>
    </div>
  
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.10/jquery-ui.js"></script>
  <script src="../js/jolokia.js"></script>
  <script src="../../js/jolokia-simple.js"></script>
  <script src="../../js/highcharts.src.js"></script>
  <script src="../../js/dashboard.js"></script>
  <script src="../../js/periodic.js"></script>

  <script>
    $(document).ready(function() {
      var table = new JmxTable();
      table.showFromList('../../jmx',
          { type: "read", mbean: "com.rayo.gateway:Type=Gateway", attribute: "RayoNodes"},
          'hostname',
          '${node}',
          ['hostname', 'ipAddress', 'platforms', 'priority', 'weight', 'consecutiveErrors', 'blacklisted'],
          ['Hostname', 'IP Address', 'Platforms', 'Priority', 'Weight', 'Consecutive Errors', 'Blacklisted'],
          '.information');
      });
  </script>
  </body>
</html>



 
 
 
function JmxTable() {

    this.showFromBean = function (jmx, path, name, attributes, names, element) {

        var jolokia = new Jolokia(jmx);
        var response = jolokia.request(path);
        var html = "<h2>" + name + "</h2> \
                    <table> \
                    <tbody>";

        $.each(names, function(index, n) {
            html+="<tr><td>" + n + "</td>";
            html+="<td>" + response.value[attributes[index]] + "</td></tr>";
        });

        html+="      </tbody> \
                   </table>";
        $(element).html($(html));
    }
    
    this.showList = function (jmx, path, name, element) {

        var jolokia = new Jolokia(jmx);
        var response = jolokia.request(path);
        var html = "<h2>" + name + "</h2> \
                    <table> \
                    <tbody>";

        $.each(response.value, function(index, n) {
            html+="<tr><td>" + n + "</td></tr>";
        });

        html+="      </tbody> \
                   </table>";
        $(element).html($(html));
    }
    
    function showFromJmxList(jmx, path, key, name, attributes, names, element) {

        var jolokia = new Jolokia(jmx);
        var response = jolokia.request(path);
        var html = "<h2>" + name + "</h2> \
                    <table> \
                    <tbody>";

        $.each(response.value, function(index, item) {
            var elementName = item[key];
            if (elementName == name) {
                $.each(names, function(index, n) {
                    html+="<tr><td>" + n + "</td>";
                    html+="<td>" + item[attributes[index]] + "</td></tr>";
                });
            }
        });

        html+="      </tbody> \
                   </table>";
        $(element).html($(html));
    }

    this.showFromList = function(jmx, path, key, name, attributes, names, element) {

        showFromJmxList(jmx, path, key, name, attributes, names, element);
    };


	function invokeJmx(jmx, header,paths, names, element) {

		var jolokia = new Jolokia(jmx);
		var responses = jolokia.request(paths);
		  var html = "<h2>" + header + "</h2> \
					 <table> \
					   <tbody>";

		  $.each(responses, function(index, response) { 
			html+="<tr><td>" + names[index] + "</td>";
			html+="<td>" + response.value + "</td></tr>";
		  });


		  html+="      </tbody> \
					 </table>";	
		  $(element).html($(html));
    }

	function linkJmxList(jmx, header, path, name, link, element) {

		var jolokia = new Jolokia(jmx);
		var response = jolokia.request(path);
		console.log(response.value);
		var html = "<h2>" + header + "</h2> \
					<table> \
					  <tbody>";

		  $.each(response.value, function(index, element) {
			var elementName = element[name];
			var target = link + "/" + elementName; 
			html+="<tr><td><a href='"+ target+"'>"+elementName + "</a></td></tr>";
		  });

		  html+="      </tbody> \
					 </table>";	
		  $(element).html($(html));
    }
    
	this.create = function(jmx, header, paths, names, element) {
	  
	  invokeJmx(jmx, header, paths, names, element);
	  $.periodic({period: 1000}, function() {
		invokeJmx(jmx, header, paths, names, element);
	  });
	};


	this.createFromList = function(jmx, header, path, name, link, element) {
	  
	  linkJmxList(jmx, header, path, name, link, element);
	};
}

function JmxChartsFactory(jmx, keepHistorySec, pollInterval, columnsCount) {
	var jolokia = new Jolokia(jmx);
	var series = [];
	var monitoredMbeans = [];
	var chartsCount = 0;

	columnsCount = columnsCount || 3;
	pollInterval = pollInterval || 1000;
	var keepPoints = (keepHistorySec || 600) / (pollInterval / 1000);

	setupPortletsContainer(columnsCount);

	setInterval(function() {
		pollAndUpdateCharts();
	}, pollInterval);

	this.create = function(mbeans) {
		mbeans = $.makeArray(mbeans);
		series = series.concat(createChart(mbeans).series);
		monitoredMbeans = monitoredMbeans.concat(mbeans);
	};

	function pollAndUpdateCharts() {
		var requests = prepareBatchRequest();
		var responses = jolokia.request(requests);
		updateCharts(responses);
	}

	function createNewPortlet(name) {
		return $('#portlet-template')
				.clone(true)
				.appendTo($('.column')[chartsCount++ % columnsCount])
				.removeAttr('id')
				.find('.title').text((name.length > 50? '...' : '') + name.substring(name.length - 50, name.length)).end()
				.find('.portlet-content')[0];
	}

	function setupPortletsContainer() {
		var column = $('.column');
		for(var i = 1; i < columnsCount; ++i){
			column.clone().appendTo(column.parent());
		}
		$(".column").sortable({
			connectWith: ".column"
		});

		$(".portlet-header .ui-icon").click(function() {
			$(this).toggleClass("ui-icon-minusthick").toggleClass("ui-icon-plusthick");
			$(this).parents(".portlet:first").find(".portlet-content").toggle();
		});
		$(".column").disableSelection();
	}

	function prepareBatchRequest() {
		return $.map(monitoredMbeans, function(mbean) {
			return {
				type: "read",
				mbean: mbean.name,
				attribute: mbean.attribute,
				path: mbean.path
			};
		});
	}

	function updateCharts(responses) {
		var curChart = 0;
		$.each(responses, function() {
			var point = {
				x: this.timestamp * 1000,
				y: parseFloat(this.value)
			};
			var curSeries = series[curChart++];
			curSeries.addPoint(point, true, curSeries.data.length >= keepPoints);
		});
	}

	function createChart(mbeans) {
		return new Highcharts.Chart({
			chart: {
				renderTo: createNewPortlet(mbeans[0].name),
				animation: false,
				defaultSeriesType: 'area',
				shadow: false
			},
			title: { text: null },
			xAxis: { type: 'datetime' },
			yAxis: {
				title: { text: mbeans[0].attribute }
			},
			legend: {
				enabled: true,
				borderWidth: 0
			},
			credits: {enabled: false},
			exporting: { enabled: false },
			plotOptions: {
				area: {
					marker: {
						enabled: false
					}
				}
			},
			series: $.map(mbeans, function(mbean) {
				return {
					data: [],
					name: mbean.path || mbean.attribute
				}
			})
		})
	}
}

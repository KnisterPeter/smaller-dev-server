Smaller Development Server
==========================

Summary
-------
The smaller-dev-server acts as proxy to some web application but intercepts 
configured requests to process javascript, css and images. It 
uses [Smaller](https://github.com/KnisterPeter/Smaller) to utilize all the tools to process resources.

This enables frontend developers to build upon all the cool frontend dev tools
while don't conflict with the server-side development build-tools. 
Also allow prototype development with easy reuse in development later on without change in technology.

![](http://knisterpeter.github.io/smaller-dev-server/Smaller%20Development%20Server%20Architecture%20Chart.svg)

Configuration / Commandline Parameter
-------------------------------------

    java -jar smaller-dev-server-0.0.1-SNAPSHOT.jar --proxyhost localhost 
      --proxyport 3000 --processors "coffeeScript,browserify,lessjs" 
      --in "main.coffee" --in "main.less" --process "/app.js" 
      --process "/style.css" --document-root src/test/resources/
      ----template-engine raw


    --ip VAL                   : The ip to bind to - defaults to 0.0.0.0
    --port N                   : The port to bind to - defaults to 12345
    --proxyhost VAL            : The host to proxy
    --proxyport N              : The port to proxy - defaults to 80
    -P (--process) VAL         : The requests to intercept
    -d (--document-root) FILE  : The folders to scan for resources
    -h (--help)                : This screen
    -i (--in) VAL              : The main input files if any
    -l (--live-reload)         : Flag to enable live-reload feature
    -p (--processors) VAL      : To processors to apply to intercepted requests
    -t (--template-engine) VAL : The template engine to use. Could be one of:
                                   raw        - Just deliveres raw html files
                                   soy        - Google Closure templates
                                   velocity   - Apache Velocity templates
                                   handlebars - Handlebars templates
    -v (--verbose)             : To log debug info

Template Data and Mock Data Server
----------------------------------
For a request there could be a <request.path>.cfg.js file which contains some
json data structure providing template data or json data as request-response.

For example if there is a request to '/page/detail.html' the config file
for this request would be '/page/detail.html.cfg.js'.
The configuration in this file is structured like this:

    {
      // Request parameters order by key and if multiple values per key, the 
      // values are ordered as well.
      // Using this one could return different responses based on the request
      // parameters
      "": {
        // These are the data which is given to the template engine
        "templateData" : {
          "key" : "value"
          "map" : {}
          "list": []
        }
      },
      "template=other": {
        // The template path is optional and could be used to specify a template
        // which is located under a differnt name or path as the request
        "templatePath" : "/sub/other.template",
        // The template name is used by the closure-templates only to define the
        // template method to call.
        "templateName" : "namespace.renderOther"
      },
      "response=json": {
        // The key jsonResponse will define the json response object
        // If defined no template will be rendered, but the json object is returned
        "jsonResponse" : {
          "a":"b"
        }
      }
    }

Credits
-------

Thanks to [SinnerSchrader](http://www.sinnerschrader.com/) for their support
and the time to work on this project.


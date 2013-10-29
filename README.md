The smaller-dev-server acts as proxy to some web application but intercepts 
configured requests to process javascript, css and images.

    java -jar smaller-dev-server-0.0.1-SNAPSHOT.jar --proxyhost localhost 
      --proxyport 3000 --processors "coffeeScript,browserify,lessjs" 
      --in "main.coffee" --in "main.less" --process "/app.js" 
      --process "/style.css" --document-root src/test/resources/


    --ip VAL                  : The ip to bind to - defaults to 0.0.0.0
    --port N                  : The port to bind to - defaults to 12345
    --proxyhost VAL           : The host to proxy
    --proxyport N             : The port to proxy - defaults to 80
    -P (--process) VAL        : The requests to intercept
    -d (--document-root) FILE : The folders to scan for resources
    -h (--help)               : This screen
    -i (--in) VAL             : The main input files if any
    -p (--processors) VAL     : To processors to apply to intercepted requests
    -v (--verbose)            : To log debug info

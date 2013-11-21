var miniJasmineLib = require('minijasminenode');
var fs = require('fs');
var path = require('path');
var file = require('file');
var given = require('jasmine-given');

function getAllFiles(indir) {
  var expr = new RegExp('.*_spec.*$');
  var queue = [];
  file.walkSync(indir, function(dir, dirs, files) {
    files.forEach(function(f) {
      if (expr.test(f)) {
        queue.push(path.relative(indir, path.join(dir, f)));
      }
    });
  });
  return queue;
}

var JsonReporter = function(callback) {
  this.specs = [];
  this.callback = callback;
};
JsonReporter.prototype = {
    reportRunnerStarting: function(runner) {
      this.start = new Date().getTime();
      this.executed_specs = 0;
      this.passed_specs = 0;
      this.executed_asserts = 0;
      this.passed_asserts = 0;
    },
    reportSpecStarting: function(spec) {
      this.executed_specs++;
    },
    reportSpecResults: function(spec) {
      var obj = {
        id: spec.id,
        description: spec.description
      };
      
      var results = spec.results();
      if (results.skipped) {
        obj.skipped = true;
      } else {
        var passed = results.passed();
        this.passed_asserts += results.passedCount;
        this.executed_asserts += results.totalCount;
        obj.passed = passed;
        if (passed) {
          this.passed_specs++;
        } else {
          var items = results.getItems();
          obj.stack = items.map(function(item) {
            if (item.trace) {
              return item.trace.stack? item.trace.stack : item.message;
            }
            return item.message;
          });
        }
      }
      this.specs.push(obj);
    },
    reportSuiteResults: function(suite) {
    },
    reportRunnerResults: function(runner) {
      var duration = new Date().getTime() - this.start;
      var failed = this.executed_specs - this.passed_specs;
      
      this.callback({
        result: !!this.executed_asserts,
        duration: duration,
        stats: {
          executed: this.executed_specs,
          passed: this.passed_specs,
          failed: failed
        },
        specs: this.specs
      });
    },
    log: function(str) {}
};

module.exports = function(command, done) {
  process.chdir(command.indir);
  
  var env = jasmine.getEnv();
  env.addReporter(new JsonReporter(function(results) {
    console.log(results);
    var target = path.join(command.outdir, 'output.json');
    fs.writeFile(target, JSON.stringify(results), function() {
      done('output.json');
    });
  }));
  env.addReporter = function() {};

  miniJasmineLib.executeSpecs({
    specs: getAllFiles(command.indir),
    includeStackTrace: true,
    defaultTimeoutInterval: 5000,
    jasmineEnv: env
  });


};

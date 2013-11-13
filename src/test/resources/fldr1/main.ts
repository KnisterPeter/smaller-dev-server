//var dep1 = require('./mod1')

class Main {
  dep: any
  
  constructor() {
    this.dep = {modfunc: function() {return 2}};
  }
    
  render(el) {
    el.innerHTML = this.dep.modfunc(Math.random() * 10);
  }
}

new Main().render(document.getElementById('content'))

dep1 = require './mod1'

class Main
  constructor:() ->
    @dep = new dep1()
    
  render: (el) ->
    el.innerHTML = @dep.modfunc(Math.random() * 10);

new Main().render(document.getElementById('content'))

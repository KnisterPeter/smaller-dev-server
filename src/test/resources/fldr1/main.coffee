dep1 = require './mod1'

class Main
  constructor:() ->
    @dep = new dep1()
    
  render: (el) ->
    el.innerHTML = @dep.modfunc(1);

new Main().render(document.getElementById('content'))

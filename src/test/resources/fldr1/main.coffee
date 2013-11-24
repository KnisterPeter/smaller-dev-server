dep1 = require './mod1'

class Main
  constructor: (el) ->
    @el = el
    @dep = new dep1()
    
  render: () ->
    @el.innerHTML = @dep.modfunc(Math.random() * 10);

el = document.getElementById('content')
if (!el.main)
  el.main = new Main(el)
  console.log('Created new Main')
else
  console.log('Reusing main')
el.main.render()

Mod1 = require('./mod1');

describe "Mod1", ->
  Given -> @subject = new Mod1
  
  describe "#modfunc", ->
    When -> @result = @subject.modfunc(1)
    Then -> @result == 2

  describe "#modfund false", ->
    When -> @result = 2
    Then -> @result == 3

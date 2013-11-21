dep1 = require './mod2'
dep2 = require './mod3'

class Mod1
  modfunc: (x) -> x * 2

module.exports = Mod1

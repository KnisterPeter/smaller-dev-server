var Mod1 = require('./mod1');

describe("Mod1", function() {
  it("modfunc should double the input", function() {
    expect(new Mod1().modfunc(1)).toBe(2);
  });
});

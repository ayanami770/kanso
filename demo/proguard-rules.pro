# The demo is minified so R8 actually runs over kanso's output on every CI run.
#
# kanso's consumer-rules.pro asserts that a pure-Compose library needs no keep rules. Until the
# demo was minified nothing ever executed that assertion — this file is empty because the
# assertion holds, and if it stops holding the demo will fail to build rather than a consuming
# app discovering it at release time.

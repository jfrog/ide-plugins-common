module test-module

go 1.16

require (
	a v1.0.0
	b v1.0.0
	c v1.0.0
	d v1.0.0
)

replace a => ../a

replace b/v2 => ../b1

replace c => c v1.0.1

replace d => /some/absolute/path/unix

package main

import (
	"flag"
	"os"
)

// This program searches and replaces relative paths in go.mod files to absolute paths.
// For example:
// go.mod before running this program:
// ...
// replace github.com/jfrog/jfrog-client-go v1.2.3 => github.com/jfrog/jfrog-client-go v1.2.4
// replace github.com/jfrog/jfrog-cli-core => ../jfrog-cli-core
//
// Running the following command:
// > go run . -goModPath=/Users/frogger/code/jfrog-cli/go.mod -wd=/Users/frogger/code/jfrog-cli
//
// go.mod after running this program:
// ...
// replace github.com/jfrog/jfrog-client-go v1.2.3 => github.com/jfrog/jfrog-client-go v1.2.4
// replace github.com/jfrog/jfrog-cli-core => /Users/frogger/code/jfrog-cli-core
func main() {
	goModPath := flag.String("goModPath", "", "Path to go.mod")
	workingDir := flag.String("wd", "", "Path to working directory")
	flag.Parse()
	if *goModPath == "" || *workingDir == "" {
		flag.Usage()
		os.Exit(1)
	}

	args, err := prepareArgs(*goModPath, *workingDir)
	exitIfError(err)
	exitIfError(absolutize(args))
}

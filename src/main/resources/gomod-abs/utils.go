package main

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"

	"golang.org/x/mod/modfile"
)

type args struct {
	// Path to go.mod file
	goModPath string
	// The working directory which will be concatenated to the relative path in the go.mod file
	workingDir string
}

/*
  Create args struct from the input args
  goModPath  - Path to go.mod file
  workingDir - The working directory which will be concatenated to the relative path in the go.mod file
*/
func prepareArgs(goModPath, workingDir string) (*args, error) {
	absWorkingDir, err := filepath.Abs(workingDir)
	if err != nil {
		return nil, errors.New("Couldn't absolutize working directory " + workingDir)
	}
	return &args{
		goModPath:  goModPath,
		workingDir: absWorkingDir,
	}, nil
}

/*
  Make relatie path in go.mod absolute
  args - The input arguments
*/
func absolutize(args *args) error {
	file, err := parseGoMod(args.goModPath)
	if err != nil {
		return errors.New("Couldn't parse go.mod file " + args.goModPath)
	}

	if err = replaceRelativePaths(file, args.workingDir); err != nil {
		return errors.New("Couldn't replace path to absolute in " + args.goModPath)
	}

	if err = saveGoMod(file); err != nil {
		return errors.New("Couldn't save go.mod in " + args.goModPath)
	}
	return nil
}

/*
  Parse go.mod file and return structurized object
  goModPath - Path to go.mod file
*/
func parseGoMod(goModPath string) (*modfile.File, error) {
	goModContent, err := os.ReadFile(goModPath)
	if err != nil {
		return nil, err
	}
	modfile, err := modfile.Parse(goModPath, goModContent, nil)
	if err != nil {
		return nil, err
	}

	return modfile, nil
}

/*
  Replace relative paths in go.mod to absolute paths
  file       - Parsed go.mod file
  workingDir - The working directory which will be concatenated to the relative path in the go.mod file
*/
func replaceRelativePaths(file *modfile.File, workingDir string) error {
	for _, replace := range file.Replace {
		currentPath := replace.New.Path
		if replace.New.Version == "" && !filepath.IsAbs(currentPath) {
			absPath := filepath.Clean(filepath.Join(workingDir, currentPath))
			fmt.Println("Replacing " + currentPath + " with " + absPath)
			if err := file.AddReplace(replace.Old.Path, "", absPath, ""); err != nil {
				return err
			}
		}
	}
	return nil
}

/*
  Save changes in go.mod file
  file - Parsed go.mod file
*/
func saveGoMod(file *modfile.File) error {
	content, err := file.Format()
	if err != nil {
		return err
	}
	return os.WriteFile(file.Syntax.Name, content, 0600)
}

/*
  Exit with error code 1 if the input error is not nil
  err - The error to check
*/
func exitIfError(err error) {
	if err == nil {
		return
	}
	fmt.Println(err.Error())
	os.Exit(1)
}

package main

import (
	"io/ioutil"
	"os"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"golang.org/x/mod/module"
)

func TestAbsolutizeUnix(t *testing.T) {
	if os.PathSeparator != '/' {
		t.Skip("Skipping test on Windows")
	}
	args := prepareGoMod(t, "unix", "/test/dummy/abs")
	defer os.Remove(args.goModPath)
	assert.NoError(t, absolutize(args))

	// Check results
	goMod, errs := parseGoMod(args.goModPath)
	assert.Nil(t, errs)
	assert.Equal(t, module.Version{Path: "/test/dummy/a", Version: ""}, goMod.Replace[0].New)
	assert.Equal(t, module.Version{Path: "/test/dummy/a", Version: ""}, goMod.Replace[0].New)
	assert.Equal(t, module.Version{Path: "c", Version: "v1.0.1"}, goMod.Replace[2].New)
	assert.Equal(t, module.Version{Path: "/some/absolute/path/unix", Version: ""}, goMod.Replace[3].New)
}

func TestAbsolutizeWin(t *testing.T) {
	if os.PathSeparator != '\\' {
		t.Skip("Skipping test on Unix")
	}
	args := prepareGoMod(t, "windows", "C:\\test\\dummy\\abs")
	defer os.Remove(args.goModPath)
	assert.NoError(t, absolutize(args))

	// Check results
	goMod, errs := parseGoMod(args.goModPath)
	assert.Nil(t, errs)
	assert.Equal(t, module.Version{Path: "C:\\test\\dummy\\a", Version: ""}, goMod.Replace[0].New)
	assert.Equal(t, module.Version{Path: "C:\\test\\dummy\\a", Version: ""}, goMod.Replace[0].New)
	assert.Equal(t, module.Version{Path: "c", Version: "v1.0.1"}, goMod.Replace[2].New)
	assert.Equal(t, module.Version{Path: "C:\\some\\absolute\\path\\", Version: ""}, goMod.Replace[3].New)
}

func TestErroneousGoMod(t *testing.T) {
	goMod, errs := parseGoMod(filepath.Join("testdata", "erroneous", "go.mod"))
	assert.Len(t, errs, 1)
	assert.Error(t, errs[0])
	assert.Nil(t, goMod)
}

func TestBadGoModPath(t *testing.T) {
	goMod, errs := parseGoMod(filepath.Join("testdata", "nonexist", "go.mod"))
	assert.Len(t, errs, 1)
	assert.ErrorIs(t, errs[0], os.ErrNotExist)
	assert.Nil(t, goMod)
}

func prepareGoMod(t *testing.T, goModDir, workingDir string) *args {
	goModPath, err := ioutil.TempFile("", "go.mod")
	assert.NoError(t, err)
	bytesRead, err := ioutil.ReadFile(filepath.Join("testdata", goModDir, "go.mod"))
	assert.NoError(t, err)
	err = ioutil.WriteFile(goModPath.Name(), bytesRead, 0644)
	assert.NoError(t, err)
	return &args{goModPath: goModPath.Name(), workingDir: workingDir}
}

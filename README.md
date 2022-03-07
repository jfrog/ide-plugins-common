# Ide Plugins Common [![Build status](https://github.com/jfrog/ide-plugins-common/actions/workflows/test.yml/badge.svg)](https://github.com/jfrog/ide-plugins-common/actions/workflows/test.yml)

This project includes the common code used by the [JFrog Idea Plugin](https://github.com/jfrog/jfrog-idea-plugin) and the [JFrog Eclipse plugin](https://github.com/jfrog/jfrog-eclipse-plugin).

# Building and Testing the Sources
After cloning the project, update submodules:
```
git submodule init
git submodule update
```
To build the code using the Gradle wrapper in Linux/Unix run:  
```
> ./gradlew clean build
```
To build the code using the Gradle wrapper in Windows run:  
```
> gradlew clean build
```
To build the code using the environment Gradle run:  
```
> gradle clean build
```
To build the code without running the tests, add to the "clean build" command the "-x test" option, for example:
```
> ./gradlew clean build -x test
```

# Code Contributions
We welcome community contribution through pull requests.

# Release Notes
The release notes are available [here](RELEASE.md#release-notes).

# Ide Plugins Common [![Build status](https://ci.appveyor.com/api/projects/status/alp9kna5f07yf5t2?svg=true)](https://ci.appveyor.com/project/jfrog-ecosystem/ide-plugins-common)

This project includes the common code used by the [JFrog Idea Plugin](https://github.com/jfrog/jfrog-idea-plugin) and the [JFrog Eclipse plugin](https://github.com/jfrog/jfrog-eclipse-plugin).

# Building and Testing the Sources

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

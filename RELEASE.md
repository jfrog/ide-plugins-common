# Release Notes

## 1.13.0
- Add support for yarn v1
- Improve Gradle dependencies tree building by using gradle-deps-tree library 
- Bugfix - handle build number with colon
- Bugfix - ConcurrentModificationException in cache

## 1.12.0
- Use gomod-absolutizer submodule
- Allow scanning npm projects without node_modules
- Root project should not be excluded from scanning
- Allow missing modules in Go
- Bugfix - Running AQL with wrong credentials logs non-informative message
- Bugfix - Stackoverflow in CI tree when there is a loop

## 1.11.0
- Improve progress indicator
- Support Xray watches
- Add CVE exporter
- Add ignore URL and vulnerabilities references to scan cache
- Bug fix - dependencies and artifacts without a license in the CI view are hidden

## 1.10.0
- Allow building Go dependency tree with errors
- Populate missing fields of violations and vulnerabilities

## 1.9.0
- Add access token support
- Add new JFrog CLI driver
- Improve excluded paths log messages

## 1.8.0
- Improve performance and memory consumption
- Support Go 1.15
- Show vulnerabilities and violations without summary
- CI - Support long build names and numbers
- CI - Support JFrog projects
- CI - bugfix - loadBuild failed on build names with ':'

## 1.7.2
- GraphScan - Make sure the licenses set is never empty
- GraphScan - Send to Xray a flat tree with dependencies only
- Allow providing the Go executable path
- Remove ScanLogic from the ScanManagerBase constructor

## 1.7.1

- Bugfix: go.mod and *.go files of sub Go projects should be ignored during a scanning of a project

## 1.7.0

- Improve Go scan algorithm
- Add support for the new Xray graph scan API
- Provide Gradle executable to Gradle Driver
- Update to Java 11

## 1.6.1

- Bugfix: npm without name or version throws NPE
- Error balloons should not appear on non-interactive scans

## 1.6.0

- Add support for Gradle projects
- Add PyPI component prefix
- Better support for npm 7
- Bugfix: Build patterns with slash doesn't encoded correctly

## 1.5.2 (April 29, 2021)

- Better handling checksum errors in go.sum

## 1.5.1 (April 27, 2021)

- Bugfix: Colon in build name causes errors
- Bugfix: Build scan does not properly canceled

## 1.5.0 (April 27, 2021)

- New CI integration
- Add more logs
- Add support for providing Artifactory and JFrog platform URLs
- Add loop detection mechanism in Go
- Support npm 7

## 1.4.0 (August 31, 2020)

- Allow scope filtering
- Migrate old HTTP client to PreemptiveHttpClient

## 1.3.0 (March 01, 2020)

- Add go tree-builder to support go projects

## 1.2.0 (December 30, 2019)

- Allow excluding paths from scanning npm projects
- Add support for Keystore provider to allow self-signed certificates
- Log an error when npm not in path
- Bugfix: in npm projects, XrayScanCache.json files created in workspace

## 1.1.2 (October 10, 2019)

- Allow providing environment variables in Npm Tree Builder

## 1.1.1 (August 08, 2019)

- Bug fixes

## 1.1.0 (June 25, 2019)

- Support Fixed Versions for issues
- Support http proxy
- Bug fixes

## 1.0.0 (Apr 07, 2019)

- Initial release

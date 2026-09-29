module projectReplaceWithShellChars

require github.com/test/subproject v0.0.0-00010101000000-000000000000

replace github.com/test/subproject => "../sub;&$'project"

go 1.13

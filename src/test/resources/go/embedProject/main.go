package embedProject

import (
	_ "embed"
	"fmt"
	"github.com/jfrog/jfrog-cli-core/artifactory/commands/curl"
	"github.com/jfrog/jfrog-cli-core/common/commands"
)

//go:embed version.txt

var buildVersion string

func main() {
	curl.NewRtCurlCommand(commands.CurlCommand{})
	fmt.Println(buildVersion)
}

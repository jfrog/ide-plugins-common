
var top_secret= "djefd3r0d3jxu3229cu23jo92r2fwf"

var second_api_key= "cxjojx23opu9d3290xq!";

function checkForVulnerabilities(jsCode) {
    const vulnerabilities = [];

    // Check for eval() usage (potential for XSS or code injection)
    if (/eval\(/.test(jsCode)) {
        vulnerabilities.push('eval() usage detected (potential XSS risk)');
    }

    // Check for innerHTML usage (potential for XSS if user input is inserted without sanitization)
    if (/innerHTML\s*\=/.test(jsCode)) {
        vulnerabilities.push('innerHTML assignment detected (potential XSS risk)');
    }

    // Check for usage of document.write (this can be dangerous)
    if (/document\.write\(/.test(jsCode)) {
        vulnerabilities.push('document.write() detected (potential security risk)');
    }

    // Check for direct DOM manipulation with user input (this needs to be carefully reviewed)
    if (/document\.getElementById\(\s*['"][^'"]+['"]\s*\)\s*\.\s*(value|innerText|textContent)\s*\=/.test(jsCode)) {
        vulnerabilities.push('Direct DOM manipulation with user input detected (potential XSS risk)');
    }

    // Return vulnerabilities found
    return vulnerabilities;
}

// Example JavaScript code to test
const jsCodeToTest = `
    let userInput = getUserInput();
    document.getElementById('output').innerHTML = userInput; // Potential XSS vulnerability
    eval("alert('XSS')"); // Potential XSS vulnerability
`;

// Run the static analysis
const foundVulnerabilities = checkForVulnerabilities(jsCodeToTest);

// Output the result
if (foundVulnerabilities.length > 0) {
    console.log('Vulnerabilities detected:');
    foundVulnerabilities.forEach(vuln => console.log('- ' + vuln));
} else {
    console.log('No vulnerabilities found.');
}

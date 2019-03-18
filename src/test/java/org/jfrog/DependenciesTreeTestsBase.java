package org.jfrog;

import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Severity;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author yahavi
 */
public class DependenciesTreeTestsBase {
    protected DependenciesTree root, one, two, three, four, five;

    /**
     * Build an empty tree with 5 nodes
     */
    public void init() {
        root = new DependenciesTree("0");
        one = new DependenciesTree("1");
        two = new DependenciesTree("2");
        three = new DependenciesTree("3");
        four = new DependenciesTree("4");
        five = new DependenciesTree("5");
        root.add(one); // 0 -> 1
        root.add(two); // 0 -> 2
        two.add(three); // 2 -> 3
        two.add(four); // 2 -> 4
        four.add(five); // 4 -> 5
    }

    /**
     * Create a random issue
     *
     * @param severity the issue severity
     * @return the random issue
     */
    protected Issue createIssue(Severity severity) {
        return new Issue(generateUID(), generateUID(), generateUID(), generateUID(), severity, generateUID());
    }

    /**
     * Create a license
     *
     * @param name the license name
     * @return the license
     */
    protected License createLicense(String name) {
        return new License(new ArrayList<>(), name, name, new ArrayList<>());
    }

    private String generateUID() {
        return UUID.randomUUID().toString();
    }
}

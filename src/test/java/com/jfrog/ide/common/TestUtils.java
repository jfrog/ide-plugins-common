package com.jfrog.ide.common;

import org.jfrog.build.extractor.scan.DependencyTree;

import static org.testng.Assert.assertNotNull;

/**
 * @author yahavi
 **/
public class TestUtils {

    /**
     * Get the dependency tree child. Fail the test if it doesn't exist.
     *
     * @param node      - The dependency tree
     * @param childName - The child name to search
     * @return the dependency tree child.
     */
    public static DependencyTree getAndAssertChild(DependencyTree node, String childName) {
        DependencyTree childNode = node.getChildren().stream()
                .filter(child -> childName.equals(child.getUserObject()))
                .findAny()
                .orElse(null);
        assertNotNull(childNode, "Couldn't find node '" + childName + "' between " + node + ".");
        return childNode;
    }
}

package com.jfrog.ide.common;

import org.jfrog.build.extractor.scan.DependencyTree;

import static org.testng.Assert.assertNotNull;

/**
 * @author yahavi
 **/
public class TestUtils {
    public static DependencyTree getAndAssertChild(DependencyTree node, String childName) {
        DependencyTree childNode = node.getChildren().stream()
                .filter(child -> childName.equals(child.getUserObject()))
                .findAny()
                .orElse(null);
        assertNotNull(childNode, "Couldn't find node '" + childName + "' between " + node + ".");
        return childNode;
    }
}

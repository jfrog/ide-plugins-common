package com.jfrog.ide.common;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.jfrog.build.extractor.scan.DependencyTree;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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

    /**
     * Get the dependency tree child. Fail the test if it doesn't exist.
     *
     * @param depTree   the dependency tree
     * @param childName the child name to search
     * @return the dependency tree child.
     */
    public static DepTreeNode getAndAssertChild(DepTree depTree, DepTreeNode parent, String childName) {
        assertTrue(parent.getChildren().contains(childName));
        DepTreeNode childNode = depTree.nodes().get(childName);
        assertNotNull(childNode, "Couldn't find node '" + childName + "'.");
        return childNode;
    }
}

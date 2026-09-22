package com.jfrog.ide.common.deptree;

import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertTrue;

public class DepTreeTest {
    @Test
    public void twoArgConstructorHasNoModules() {
        DepTree depTree = new DepTree("root", Map.of("root", new DepTreeNode()));
        assertTrue(depTree.modules().isEmpty());
    }
}

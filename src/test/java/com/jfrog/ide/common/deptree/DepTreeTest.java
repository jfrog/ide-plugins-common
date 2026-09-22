package com.jfrog.ide.common.deptree;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DepTreeTest {
    @Test
    public void twoArgConstructorHasNoModules() {
        DepTree depTree = new DepTree("root", Map.of("root", new DepTreeNode()));
        assertTrue(depTree.modules().isEmpty());
    }

    @Test
    public void modulesArePreserved() {
        DepTreeModule module = new DepTreeModule("moda", Map.of("moda", new DepTreeNode()));
        DepTree depTree = new DepTree("root", Map.of("root", new DepTreeNode()), List.of(module));
        assertEquals(depTree.modules(), List.of(module));
        assertEquals(depTree.modules().get(0).rootId(), "moda");
    }
}

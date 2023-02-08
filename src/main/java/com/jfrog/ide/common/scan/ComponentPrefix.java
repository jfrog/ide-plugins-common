package com.jfrog.ide.common.scan;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public enum ComponentPrefix {
    PYPI("pypi://", "PyPI"),
    GAV("gav://", "Maven"),
    NPM("npm://", "npm"),
    GO("go://", "Go");

    private final String prefix;
    private final String packageTypeName;

    ComponentPrefix(String prefix, String packageTypeName) {
        this.prefix = prefix;
        this.packageTypeName = packageTypeName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPackageTypeName() {
        return packageTypeName;
    }
}

package com.jfrog.ide.common.scan;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public enum ComponentPrefix {
    GAV("gav://"),
    NPM("npm://"),
    GO("go://");

    private String prefix;

    ComponentPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}

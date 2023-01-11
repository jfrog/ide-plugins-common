package com.jfrog.ide.common.tree;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

import static com.jfrog.ide.common.utils.Utils.removeComponentIdPrefix;

/**
 * @author yahavi
 */
public class GeneralInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String componentId = "";
    private String pkgType = "";
    private String path = "";

    @SuppressWarnings("WeakerAccess")
    public GeneralInfo() {
    }

    @SuppressWarnings("unused")
    public GeneralInfo(String componentId, String path, String pkgType) {
        this.componentId = componentId;
        this.path = path;
        this.pkgType = pkgType;
    }

    public String getGroupId() {
        int colonMatches = StringUtils.countMatches(componentId, ":");
        if (colonMatches != 2) {
            return "";
        }
        return componentId.substring(0, componentId.indexOf(":"));
    }

    public String getArtifactId() {
        String compIdWithoutPrefix = getComponentIdWithoutPrefix();
        int colonMatches = StringUtils.countMatches(compIdWithoutPrefix, ":");
        if (colonMatches < 1 || colonMatches > 2) {
            return "";
        }
        int indexOfColon = compIdWithoutPrefix.indexOf(":");
        if (colonMatches == 1) {
            return compIdWithoutPrefix.substring(0, indexOfColon);
        }
        return compIdWithoutPrefix.substring(indexOfColon + 1, compIdWithoutPrefix.lastIndexOf(":"));
    }

    public String getVersion() {
        String compIdWithoutPrefix = getComponentIdWithoutPrefix();
        int colonMatches = StringUtils.countMatches(compIdWithoutPrefix, ":");
        if (colonMatches < 1 || colonMatches > 2) {
            return "";
        }
        return compIdWithoutPrefix.substring(compIdWithoutPrefix.lastIndexOf(":") + 1);
    }

    @SuppressWarnings("unused")
    public String getComponentId() {
        return componentId;
    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public String getPkgType() {
        return pkgType;
    }

    @SuppressWarnings("unused")
    public GeneralInfo componentId(String componentId) {
        this.componentId = componentId;
        return this;
    }

    public GeneralInfo path(String path) {
        this.path = path;
        return this;
    }

    @SuppressWarnings("unused")
    public GeneralInfo pkgType(String pkgType) {
        this.pkgType = pkgType;
        return this;
    }

    public String getComponentIdWithoutPrefix() {
        return removeComponentIdPrefix(this.componentId);
    }
}
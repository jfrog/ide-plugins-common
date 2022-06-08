package com.jfrog.ide.common.ci;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents general build information.
 * Each root node in the build dependency tree will be populated with BuildGeneralInfo.
 *
 * @author yahavi
 **/
public class BuildGeneralInfo extends GeneralInfo implements ProducerConsumerItem {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Build.STARTED_FORMAT);

    public enum Status {PASSED, FAILED, UNKNOWN}

    private String buildName;
    private String buildNumber;
    private Status status;
    private Date started;
    private Vcs vcs;

    public String getBuildName() {
        return buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public Status getStatus() {
        return status;
    }

    public Date getStarted() {
        return started;
    }

    public Vcs getVcs() {
        return vcs;
    }

    public BuildGeneralInfo buildName(String buildName) {
        this.buildName = buildName;
        return this;
    }

    public BuildGeneralInfo buildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
        return this;
    }

    public BuildGeneralInfo status(String status) {
        switch (status) {
            case "PASS":
                this.status = Status.PASSED;
                break;
            case "FAIL":
                this.status = Status.FAILED;
                break;
            default:
                this.status = Status.UNKNOWN;
        }
        return this;
    }

    public BuildGeneralInfo started(String started) throws ParseException {
        this.started = DATE_FORMAT.parse(started);
        return this;
    }

    public BuildGeneralInfo vcs(Vcs vcs) {
        this.vcs = vcs;
        return this;
    }

    @Override
    public String getArtifactId() {
        return StringUtils.substringBeforeLast(getComponentId(), ":");
    }

    @Override
    public String getComponentId() {
        return buildName + ":" + buildNumber;
    }

    @Override
    public GeneralInfo componentId(String componentId) {
        return this;
    }
}

package com.jfrog.ide.common.ci;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author yahavi
 **/
public class BuildGeneralInfo extends GeneralInfo {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Build.STARTED_FORMAT);

    public enum Status {PASSED, FAILED, UNKNOWN}

    private Status status;
    private Date started;
    private Vcs vcs;

    public Status getStatus() {
        return status;
    }

    public Date getStarted() {
        return started;
    }

    public Vcs getVcs() {
        return vcs;
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

    public BuildGeneralInfo started(long started) {
        this.started = new Date(started);
        return this;
    }

    public BuildGeneralInfo vcs(Vcs vcs) {
        this.vcs = vcs;
        return this;
    }
}

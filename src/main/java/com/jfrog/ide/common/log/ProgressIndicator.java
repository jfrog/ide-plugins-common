package com.jfrog.ide.common.log;

import com.jfrog.xray.client.services.scan.ScanGraphProgress;

/**
 * @author yahavi
 */
public interface ProgressIndicator extends ScanGraphProgress {
    void setFraction(double fraction);

    void setIndeterminate(boolean indeterminate);

    void setText(String title);
}

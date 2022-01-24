package com.jfrog.ide.common.log;

/**
 * @author yahavi
 */
public interface ProgressIndicator {
    void setFraction(double fraction);

    void setIndeterminate(boolean indeterminate);

    void setText(String title);
}

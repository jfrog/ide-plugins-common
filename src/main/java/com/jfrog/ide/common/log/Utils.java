package com.jfrog.ide.common.log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;

/**
 * @author yahavi
 **/
public class Utils {

    public static void logError(Log logger, String message, Exception e, boolean shouldToast) {
        if (StringUtils.isNotBlank(message)) {
            message += ": ";
        }
        message += ExceptionUtils.getRootCauseMessage(e);
        if (shouldToast) {
            logger.error(message, e);
        } else {
            logger.warn(message);
        }
    }

    public static void logError(Log logger, String message, boolean shouldToast) {
        if (shouldToast) {
            logger.error(message);
        } else {
            logger.warn(message);
        }
    }
}

package com.jfrog.ide.common.filter;

import com.google.common.collect.Lists;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Severity;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author yahavi
 */
public class Utils {

    /**
     * Create a random issue
     *
     * @param severity the issue severity
     * @return the random issue
     */
    static Issue createIssue(Severity severity) {
        return new Issue(generateUID(), generateUID(), generateUID(), generateUID(), severity, generateUID(), Lists.newArrayList());
    }

    /**
     * Create a license
     *
     * @param name the license name
     * @return the license
     */
    static License createLicense(String name) {
        return new License(new ArrayList<>(), name, name, new ArrayList<>());
    }

    private static String generateUID() {
        return UUID.randomUUID().toString();
    }

}

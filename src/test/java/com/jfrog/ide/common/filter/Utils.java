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
    public static Issue createIssue(Severity severity) {
        return new Issue(generateUID(), severity, generateUID(), Lists.newArrayList(), Lists.newArrayList(),
                Lists.newArrayList(), generateUID());
    }

    /**
     * Create a license
     *
     * @param name the license name
     * @return the license
     */
    static License createLicense(String name) {
        return new License(name, name, new ArrayList<>());
    }

    private static String generateUID() {
        return UUID.randomUUID().toString();
    }

}

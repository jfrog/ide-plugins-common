package com.jfrog.ide.common.nodes.subentities;

/**
 * @author yahavi
 **/
public class Cve {
    private String cveId;
    private String cvssV2Score;
    private String cvssV2Vector;
    private String cvssV3Score;
    private String cvssV3Vector;

    @SuppressWarnings("unused")
    public Cve() {
    }

    @SuppressWarnings("unused")
    public Cve(String cveId, String cvssV2Score, String cvssV2Vector, String cvssV3Score, String cvssV3Vector) {
        this.cveId = cveId;
        this.cvssV2Score = cvssV2Score;
        this.cvssV2Vector = cvssV2Vector;
        this.cvssV3Score = cvssV3Score;
        this.cvssV3Vector = cvssV3Vector;
    }

    @SuppressWarnings("unused")
    public String getCveId() {
        return cveId;
    }

    @SuppressWarnings("unused")
    public String getCvssV2Score() {
        return cvssV2Score;
    }

    @SuppressWarnings("unused")
    public String getCvssV2Vector() {
        return cvssV2Vector;
    }

    @SuppressWarnings("unused")
    public String getCvssV3Score() {
        return cvssV3Score;
    }

    @SuppressWarnings("unused")
    public String getCvssV3Vector() {
        return cvssV3Vector;
    }
}

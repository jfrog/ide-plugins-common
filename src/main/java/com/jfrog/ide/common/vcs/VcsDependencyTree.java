package com.jfrog.ide.common.vcs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.extractor.scan.DependencyTree;

/**
 * @author yahavi
 **/
@Getter
@Setter
@AllArgsConstructor
public class VcsDependencyTree implements ProducerConsumerItem {
    private DependencyTree branchDependencyTree;
    private String branch;
}

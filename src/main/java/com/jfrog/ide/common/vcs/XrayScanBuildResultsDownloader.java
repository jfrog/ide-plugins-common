package com.jfrog.ide.common.vcs;

import com.google.common.collect.Multimap;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.scan.DependencyTree;

/**
 * @author yahavi
 **/
public class XrayScanBuildResultsDownloader extends ConsumerRunnableBase {
    private final Multimap<String, DependencyTree> branchDependencyTrees;
    private ProducerConsumerExecutor executor;
    private Log log;

    public XrayScanBuildResultsDownloader(Multimap<String, DependencyTree> branchDependencyTrees) {
        this.branchDependencyTrees = branchDependencyTrees;
    }

    @Override
    public void consumerRun() {
        while (!Thread.interrupted()) {
            try {
                ProducerConsumerItem item = executor.take();
                if (item == executor.TERMINATE) {
                    // If reached the TERMINATE NpmPackageInfo, return it to the queue and exit.
                    executor.put(item);
                    break;
                }
                VcsDependencyTree branchTree = (VcsDependencyTree) item;
                branchDependencyTrees.put(branchTree.getBranch(), branchTree.getBranchDependencyTree());
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void setExecutor(ProducerConsumerExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }
}

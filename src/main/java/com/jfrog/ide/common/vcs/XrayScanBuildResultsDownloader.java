package com.jfrog.ide.common.vcs;

import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.scan.DependencyTree;

/**
 * @author yahavi
 **/
public class XrayScanBuildResultsDownloader extends ConsumerRunnableBase {
    private ProducerConsumerExecutor executor;
    private final DependencyTree root;
    private Log log;

    public XrayScanBuildResultsDownloader(DependencyTree root) {
        this.root = root;
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
                DependencyTree branchTree = (DependencyTree) item;
                synchronized (root) {
                    root.add(branchTree);
                }
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

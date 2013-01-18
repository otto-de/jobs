package de.otto.jobstore.service;

import de.otto.jobstore.common.*;
import de.otto.jobstore.repository.JobInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JobExecutionRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    final JobRunnable jobRunnable;
    final JobInfoRepository jobInfoRepository;
    final JobExecutionContext context;

    JobExecutionRunnable(JobRunnable jobRunnable, JobInfoRepository jobInfoRepository, JobExecutionContext context) {
        this.jobRunnable = jobRunnable;
        this.jobInfoRepository = jobInfoRepository;
        this.context = context;
    }

    @Override
    public void run() {
        final JobDefinition jobDefinition = jobRunnable.getJobDefinition();
        final String name = jobDefinition.getName();
        try {
            LOGGER.info("ltag=JobService.JobExecutionRunnable.run start jobName={} jobId={}", name, context.getId());
            if (jobRunnable.prepare(context)) {
                jobRunnable.execute(context);
                if (!jobDefinition.isRemote()) {
                    LOGGER.info("ltag=JobService.JobExecutionRunnable.run finished jobName={} jobId={}", name, context.getId());
                    jobRunnable.afterExecution(context);
                    jobInfoRepository.markRunningAsFinished(name, context.getResultCode(), context.getResultMessage());
                }
            } else {
                LOGGER.info("ltag=JobService.JobExecutionRunnable.run skipped jobName={} jobId={}", name, context.getId());
                jobInfoRepository.markRunningAsFinished(name, ResultCode.NOT_EXECUTED, null);
            }
        } catch (Exception e) {
            LOGGER.error("ltag=JobService.JobExecutionRunnable.run jobName=" + name + " jobId=" + context.getId() + " failed: " + e.getMessage(), e);
            jobInfoRepository.markRunningAsFinishedWithException(name, e);
        }
    }

}

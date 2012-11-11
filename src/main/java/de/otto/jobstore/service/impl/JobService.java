package de.otto.jobstore.service.impl;

import de.otto.jobstore.common.*;
import de.otto.jobstore.repository.NotFoundException;
import de.otto.jobstore.repository.api.JobInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

public final class JobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    private final ConcurrentHashMap<String, JobRunnable> jobs = new ConcurrentHashMap<String, JobRunnable>();
    private final ConcurrentLinkedQueue<List<String>> runningConstraints = new ConcurrentLinkedQueue<List<String>>();

    private final JobInfoRepository jobInfoRepository;

    private final boolean executionEnabled;

    public JobService(JobInfoRepository jobInfoRepository, boolean executionEnabled) {
        this.jobInfoRepository = jobInfoRepository;
        this.executionEnabled = executionEnabled;
    }

    public void registerJob(String name, JobRunnable runnable) {
        if (jobs.containsKey(name)) {
            throw new IllegalArgumentException("job with name " + name + " already exists");
        }
        jobs.put(name, runnable);
    }

    public void executeQueuedJobs() throws Exception {
        if (executionEnabled) {
            LOGGER.info("ltag=JobService.executeQueuedJobs");
            for(final String name : jobs.keySet()) {
                executeQueuedJob(name);
            }
        }
    }

    public void addRunningConstraint(List<String> constraint) {
        for (String name : constraint) {
            if(!jobs.containsKey(name)) {
                throw new NotFoundException();
            }
        }
        runningConstraints.add(constraint);
    }

    private boolean executeQueuedJob(final String name) throws Exception {
        if (!jobs.containsKey(name)) {
            throw new NotFoundException();
        }
        // ~
        if (!jobInfoRepository.hasQueuedJob(name)) {
            LOGGER.debug("ltag=JobService.executeQueuedJob.alreadyQueue jobInfoName={}", name);
            return false;
        }
        // ~
        if (jobInfoRepository.hasRunningJob(name)) {
            LOGGER.debug("ltag=JobService.executeQueuedJob.alreadyRunning jobInfoName={}", name);
            return false;
        }
        // check running constraints
        if (!checkRunningConstraint(name)) {
            LOGGER.debug("ltag=JobService.executeQueuedJob.runningConstraintNotOk jobInfoName={}", name);
            return false;
        }
        // activate job
        if (!jobInfoRepository.activateQueuedJob(name)) {
            LOGGER.warn("ltag=JobService.executeQueuedJob.activateQueuedJob.doesNotExistAnyMore");
            return false;
        }
        jobInfoRepository.updateHostThreadInformation(name, InternetUtils.getHostName(), Thread.currentThread().getName());

        // execute async
        Executors.newSingleThreadExecutor().submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
            try {
                final JobRunnable runnable = jobs.get(name);
                LOGGER.info("ltag=JobService.executeQueuedJob.executeJob jobInfoName={}", name);
                runnable.execute(new JobLogger() {

                    @Override
                    public void addLoggingData(String log) {
                        jobInfoRepository.addLoggingData(name, log);
                    }

                    @Override
                    public void insertOrUpdateAdditionalData(String key, String value) {
                        jobInfoRepository.insertOrUpdateAdditionalData(name, key, value);
                    }

                });
                jobInfoRepository.markAsFinishedSuccessfully(name);
            } catch (Exception ex) {
                jobInfoRepository.markAsFinishedWithException(name, ex);
                throw ex;
            }
            return null;
            }
        });
        return true;
    }

    public String queueJob(String name, boolean forceExecution) {
        if (!jobs.containsKey(name)) {
            throw new NotFoundException("job with name " + name + " not found");
        }
        // ~
        final JobRunnable runnable = jobs.get(name);
        if (!forceExecution) {
            LOGGER.debug("ltag=JobService.queueJob.isExecutionNecessary jobInfoName={}", name);
            if(!runnable.isExecutionNecessary()) {
                return null;
            }
        }
        //
        if (jobInfoRepository.hasQueuedJob(name)) {
            throw new IllegalStateException("alreadyQueued jobInfoName=" + name);
        }
        //
        if (jobInfoRepository.hasRunningJob(name)) {
            throw new IllegalStateException("alreadyRunning jobInfoName=" + name);
        }
        //
        final Map<String, String> additionalData = new HashMap<String, String>();
        if (forceExecution) {
            additionalData.put("forceExecution", "TRUE");
        }
        // ~
        final String id = jobInfoRepository.create(name, runnable.getMaxExecutionTime(), RunningState.QUEUED, additionalData);
        if (id == null) {
            throw new RuntimeException("can't create job=" + name);
        }
        LOGGER.info("ltag=JobService.queueJob.queuingJob jobInfoName={}", name);
        return id;
    }

    private boolean checkRunningConstraint(String name) {
        for (List<String> constraint : runningConstraints) {
            if (!constraint.contains(name)) {
                continue;
            }
            // check constraint jobs
            for (String constraintJobName : constraint) {
                if (jobInfoRepository.hasRunningJob(constraintJobName)) {
                    return false;
                }
            }
        }
        return true;
    }

    public String queueJob(String name) {
        return queueJob(name, false);
    }

    public void removeQueuedJob(String name) {
        jobInfoRepository.removeQueuedJob(name);
    }

    public void clean() {
        jobs.clear();
        runningConstraints.clear();
    }

    public Set<String> listJobs() {
        return jobs.keySet();
    }

    //@PreDestroy
    public void stopAllJobs() {
        for (String name : jobs.keySet()) {
            LOGGER.info("ltag=JobService.stopAllJobs jobInfoName={}", name);
            if (jobInfoRepository.hasRunningJob(name)) {
                final JobInfo runningJob = jobInfoRepository.findRunningByName(name);
                if (runningJob != null && runningJob.getHost().equals(InternetUtils.getHostName())) {
                    jobInfoRepository.markAsFinished(name, ResultState.ERROR, "Executing Host was shut down");
                }
            }
        }
    }

}

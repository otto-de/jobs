package de.otto.jobstore.common;

import de.otto.jobstore.repository.JobInfoRepository;

import java.util.Map;

public class JobInfoCache {

    private final String id;
    private final JobInfoRepository jobInfoRepository;
    private long updateInterval;
    private volatile long lastUpdate = 0;
    private volatile JobInfo jobInfo;

    public JobInfoCache(String id, JobInfoRepository jobInfoRepository, long updateInterval) {
        this.id = id;
        this.jobInfoRepository = jobInfoRepository;
        this.jobInfo = getJobInfo();
        this.updateInterval = updateInterval;
    }

    public boolean isAborted() {
        return getJobInfo().isAborted();
    }

    public boolean isTimedOut() {
        return getJobInfo().isTimedOut();
    }

    public Map<String, String> getParameters() {
        return getJobInfo().getParameters();
    }

    private JobInfo getJobInfo() {
        final long currentTime = System.currentTimeMillis();
        if (lastUpdate + updateInterval < currentTime) {
            synchronized (this) {
                if (lastUpdate + updateInterval < currentTime) {
                    lastUpdate = currentTime;
                    jobInfo = jobInfoRepository.findById(id);
                }
            }
        }
        return jobInfo;
    }
}

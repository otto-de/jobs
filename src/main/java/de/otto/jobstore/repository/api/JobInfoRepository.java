package de.otto.jobstore.repository.api;

import de.otto.jobstore.common.JobInfo;
import de.otto.jobstore.common.ResultState;
import de.otto.jobstore.common.RunningState;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A repository which stores information on jobs. For each distinct job name only one job can be running or queued.
 *
 * The method {@link #cleanupTimedOutJobs} needs to be called regularly to remove possible timed out jobs which would
 * otherwise stop new jobs from being able to execute.
 */
public interface JobInfoRepository {

    /**
     * Creates a new job with the given parameters. Host and thread executing the job are determined automatically.
     *
     * @param name The name of the job
     * @param maxExecutionTime Sets the time after which a job is considered to be dead (lastModifiedTime + timeout).
     * @param state The state with which the job is started
     * @param forceExecution If a job should ignore preconditions defined on where or not it should run
     * @return The id of the job if it could be created or null if a job with the same name and state already exists
     */
    String create(String name, long maxExecutionTime, RunningState state, boolean forceExecution);

    /**
     * Creates a new job with the given parameters. Host and thread executing the job are determined automatically.
     *
     * @param name The name of the job
     * @param maxExecutionTime Sets the time after which a job is considered to be dead (lastModifiedTime + timeout).
     * @param state The state with which the job is started
     * @param forceExecution If a job should ignore preconditions defined on where or not it should run
     * @param additionalData Additional information to be stored with the job
     * @return The id of the job if it could be created or null if a job with the same name and state already exists
     */
    String create(String name, long maxExecutionTime, RunningState state, boolean forceExecution, Map<String, String> additionalData);

    /**
     * Creates a new job with the given parameters
     *
     * @param name The name of the job
     * @param host The host, on which the job is running
     * @param thread The thread, which runs the job
     * @param maxExecutionTime Sets the time after which a job is considered to be dead (lastModifiedTime + timeout).
     * @param state The state with which the job is started
     * @param forceExecution If a job should ignore preconditions defined on where or not it should run
     * @return The id of the job if it could be created or null if a job with the same name and state already exists
     */
    String create(String name, String host, String thread, long maxExecutionTime, RunningState state, boolean forceExecution);

    /**
     * Creates a new job with the given parameters
     *
     * @param name The name of the job
     * @param host The host, on which the job is running
     * @param thread The thread, which runs the job
     * @param maxExecutionTime Sets the time after which a job is considered to be dead (lastModifiedTime + timeout).
     * @param state The state with which the job is started
     * @param forceExecution If a job should ignore preconditions defined on where or not it should run
     * @param additionalData Additional information to be stored with the job
     * @return The id of the job if it could be created or null if a job with the same name and state already exists
     */
    String create(String name, String host, String thread, long maxExecutionTime, RunningState state, boolean forceExecution, Map<String, String> additionalData);

    /**
     * Returns the running job with the given name
     *
     * @param name The name of the job
     * @return The running job or null if no job with the given name is currently running
     */
    JobInfo findRunningByName(String name);

    /**
     * Checks if a job with the given name is currently running.
     *
     * @param name The name of the job
     * @return true - A job with the given name is still running<br/>
     *          false - A job with the given name is not running
     */
    boolean hasRunningJob(String name);

    /**
     * Returns the queued job with the given name
     *
     * @param name The name of the job
     * @return The queued job of null if no job with the given name is currently queued
     */
    JobInfo findQueuedByName(String name);

    /**
     * Returns all queued jobs sorted ascending by start time
     *
     * @return The queued jobs
     */
    List<JobInfo> findQueuedJobsSortedAscByCreationTime();

    /**
     * Checks if a job with the given name is currently queued
     *
     * @param name The name of the job
     * @return true - A job with the given name is queued<br/>
     *          false - A job with the given name is not queued
     */
    boolean hasQueuedJob(String name);

    /**
     * Find a job by its id
     *
     * @param id The id of the job
     * @return The job with the given id or null if no corresponding job was found.
     */
    JobInfo findById(String id);

    /**
     * Returns all jobs with the given name.
     *
     * @param name The name of the jobs
     * @return All jobs with the given name sorted descending by last modified date
     */
    List<JobInfo> findByName(String name);

    /**
     * Returns a list of jobs with the given name which have a lastModified timestamp
     * which is in between the supplied dates. If the start and end parameter are null, the result list will contain
     * all jobs with the supplied name.
     *
     * @param name The name of the jobs to return
     * @param start The date on or after which the jobs were last modified
     * @param end The date on or before which the jobs were last modified
     * @return The list of jobs sorted by startTime in descending order
     */
    List<JobInfo> findByNameAndTimeRange(String name, Date start, Date end);

    /**
     * Returns the job with the given name and the most current last modified timestamp.
     *
     * @param name The name of the job
     * @return The job with the given name and the most current timestamp or null if none could be found.
     */
    JobInfo findLastByName(String name);

    /**
     * Returns the job with the given name and result state as well as the most current last modified timestamp.
     *
     * @param name The name of the job
     * @return The job with the given name and result state as well as the most current timestamp or null
     * if none could be found.
     */
    JobInfo findLastByNameAndResultState(String name, ResultState resultState);

    /**
     * Returns for all existing job names the job with the most current last modified timestamp regardless of its state.
     *
     * @return The jobs with distinct names and the most current last modified timestamp
     */
    List<JobInfo> findLast();

    /**
     * Returns for all existing job names the job with the most current finished timestamp and a running state of
     * neither running or queued.
     *
     * @return The jobs with distinct names and the most current last finished timestamp which is not running or queued
     */
    List<JobInfo> findLastNotActive();

    /**
     * Returns the job with the given name which is not running or queued and which has the most current finished timestamp
     *
     * @param name The name of the job
     * @return The job with the given name and most current finished timestamp
     */
    JobInfo findLastNotActiveByName(String name);

    /**
     * Returns the list of all distinct job names within this repository
     *
     * @return The list of distinct jobnames
     */
    List<String> distinctJobNames();

    /**
     * Sets the status of the queued job with the given name to running. The lastModified date of the job is set
     * to the current date.
     *
     * @param name The name of the job
     * @return true - If the job with the given name was activated successfully<br/>
     *          false - If no queued job with the current name could be found and thus could not activated
     */
    boolean activateQueuedJob(String name);

    /**
     * Removes a queued job with the given name.
     *
     * @param name The name of the job
     * @return true - If the job was deleted successfully<br/>
     *          false - If no queued job with the given name could be found
     */
    boolean removeQueuedJob(String name);

    /**
     * Marks a job with the given name as finished.
     *
     * @param name The name of the job
     * @param state The result state of the job
     * @return true - The job was marked as requested<br/>
     *          false - No running job with the given name could be found
     */
    boolean markAsFinished(String name, ResultState state);

    /**
     * Marks a job with the given name as finished.
     *
     * @param name The name of the job
     * @param state The result state of the job
     * @param errorMessage An optional error message
     * @return true - The job was marked as requested<br/>
     *          false - No running job with the given name could be found
     */
    boolean markAsFinished(String name, ResultState state, String errorMessage);

    /**
     * Marks a job with the given name as finished with an error and writes the stack trace of the exception
     * to the error message property of the job.
     *
     * @param name The name of the job
     * @return true - The job was marked as requested<br/>
     *          false - No running job with the given name could be found
     */
    boolean markAsFinishedWithException(String name, Exception ex);

    /**
     * Marks a job with the given name as finished successfully.
     *
     * @param name The name of the job
     * @return true - The job was marked as requested<br/>
     *          false - No running job with the given name could be found
     */
    boolean markAsFinishedSuccessfully(String name);

    /**
     * Adds additional data to a running job with the given name. If information with the given key already exists
     * it is overwritten. The lastModified date of the job is set to the current date.
     *
     * @param name The name of the job
     * @param key The key of the data to save
     * @param value The information to save
     * @return true - The data was successfully added to the job<br/>
     *          false - No running job with the given name could be found
     */
    boolean insertAdditionalData(String name, String key, String value);

    /**
     * Updates the host and thread information on the running job with the given name. Host and thread information
     * are determined automatically.
     *
     * @param name The name of the job
     * @return true - The data was successfully added to the job<br/>
               false - No running job with the given name could be found
     */
    boolean updateHostThreadInformation(String name);

    /**
     * Updates the host and thread information on the running job with the given name
     *
     * @param name The name of the job
     * @param host The host to set
     * @param thread The thread to set
     * @return true - The data was successfully added to the job<br/>
    false - No running job with the given name could be found
     */
    boolean updateHostThreadInformation(String name, String host, String thread);

    /**
     * Adds a logging line to the logging data of the running job with the supplied name
     *
     * @param name The name of the job
     * @param line The log line to add
     * @return true - The data was successfully added to the job<br/>
     *         false - No running job with the given name could be found
     */
    boolean addLoggingData(String name, String line);

    /**
     * Clears all elements from the repository
     *
     * @param dropCollection Flag if the collection should be dropped
     */
    void clear(boolean dropCollection);

    /**
     * Marks all timed out jobs in the repository as timed out
     */
    void cleanupTimedOutJobs();

    /**
     * Counts the number of documents in the repository
     *
     * @return The number of documents in the repository
     */
    long count();

}

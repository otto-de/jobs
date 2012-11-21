package de.otto.jobstore.web;

import com.mongodb.BasicDBObject;
import com.sun.jersey.api.uri.UriBuilderImpl;
import de.otto.jobstore.common.JobInfo;
import de.otto.jobstore.common.properties.JobInfoProperty;
import de.otto.jobstore.repository.api.JobInfoRepository;
import de.otto.jobstore.service.api.JobService;
import de.otto.jobstore.service.exception.JobAlreadyQueuedException;
import de.otto.jobstore.service.exception.JobAlreadyRunningException;
import de.otto.jobstore.service.exception.JobNotRegisteredException;
import de.otto.jobstore.web.representation.JobInfoRepresentation;
import de.otto.jobstore.web.representation.JobNameRepresentation;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

public class JobInfoResourceTest {

    private JobInfoResource jobInfoResource;
    private JobService jobService;
    private JobInfoRepository jobInfoRepository;
    private UriInfo uriInfo;
    private JobInfo JOB_INFO;

    @BeforeMethod
    public void setUp() throws Exception {
        jobService = mock(JobService.class);
        jobInfoRepository = mock(JobInfoRepository.class);
        jobInfoResource = new JobInfoResource(jobInfoRepository, jobService);

        uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenReturn(new UriBuilderImpl());
        JOB_INFO = new JobInfo(new BasicDBObject().append(JobInfoProperty.ID.val(), "1234").append(JobInfoProperty.NAME.val(), "foo"));
    }

    @Test
    public void testGetJobs() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(JobNameRepresentation.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        Set<String> names = new HashSet<String>();
        names.add("foo"); names.add("bar");
        when(jobService.listJobNames()).thenReturn(names);
        Response response = jobInfoResource.getJobs(uriInfo);
        assertEquals(200, response.getStatus());
        Feed feed = (Feed) response.getEntity();

        List<Entry> entries = feed.getEntries();
        assertEquals(2, entries.size());
        Entry foo = entries.get(0);
        JobNameRepresentation fooRep = (JobNameRepresentation) unmarshaller.unmarshal(new StringReader(foo.getContent()));
        assertEquals("foo", fooRep.getName());
        Entry bar = entries.get(1);
        JobNameRepresentation barRep = (JobNameRepresentation) unmarshaller.unmarshal(new StringReader(bar.getContent()));
        assertEquals("bar", barRep.getName());
    }

    @Test
    public void testGetJobsEmpty() throws Exception {
        when(jobService.listJobNames()).thenReturn(new HashSet<String>());

        Response response = jobInfoResource.getJobs(uriInfo);
        assertEquals(200, response.getStatus());
        Feed feed = (Feed) response.getEntity();

        List<Entry> entries = feed.getEntries();
        assertEquals(0, entries.size());
    }

    @Test
    public void testExecuteJobWhichIsNotRegistered() throws Exception {
        when(jobService.executeJob("foo", true)).thenThrow(new JobNotRegisteredException(""));

        Response response = jobInfoResource.executeJob("foo", uriInfo);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testExecuteJobWhichIsAlreadyQueued() throws Exception {
        when(jobService.executeJob("foo", true)).thenThrow(new JobAlreadyQueuedException(""));

        Response response = jobInfoResource.executeJob("foo", uriInfo);
        assertEquals(409, response.getStatus());
    }

    @Test
    public void testExecuteJobWhichIsAlreadyRunning() throws Exception {
        when(jobService.executeJob("foo", true)).thenThrow(new JobAlreadyRunningException(""));

        Response response = jobInfoResource.executeJob("foo", uriInfo);
        assertEquals(409, response.getStatus());
    }

    @Test
    public void testExecuteJob() throws Exception {
        when(jobService.executeJob("foo", true)).thenReturn("1234");
        when(jobInfoRepository.findById("1234")).thenReturn(JOB_INFO);

        Response response = jobInfoResource.executeJob("foo", uriInfo);
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testGetJob() throws Exception {
        when(jobInfoRepository.findById("1234")).thenReturn(JOB_INFO);

        Response response = jobInfoResource.getJob("foo", "1234");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGetJobNotExisting() throws Exception {
        when(jobInfoRepository.findById("1234")).thenReturn(null);

        Response response = jobInfoResource.getJob("foo", "1234");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetJobMismatchingName() throws Exception {
        when(jobInfoRepository.findById("1234")).thenReturn(JOB_INFO);

        Response response = jobInfoResource.getJob("bar", "1234");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetJobsByName() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(JobInfoRepresentation.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        when(jobInfoRepository.findByName("foo", 5)).thenReturn(createJobs(5, "foo"));

        Response response = jobInfoResource.getJobsByName("foo", 5, uriInfo);
        assertEquals(200, response.getStatus());
        Feed feed = (Feed) response.getEntity();

        List<Entry> entries = feed.getEntries();
        assertEquals(5, entries.size());

        Entry foo = entries.get(0);
        JobInfoRepresentation fooRep = (JobInfoRepresentation) unmarshaller.unmarshal(new StringReader(foo.getContent()));
        assertEquals("0", fooRep.getId());
        assertEquals("foo", fooRep.getName());
    }

    @Test
    public void testGetJobsByEmpty() throws Exception {
        when(jobInfoRepository.findByName("foo", 5)).thenReturn(new ArrayList<JobInfo>());

        Response response = jobInfoResource.getJobsByName("foo", 5, uriInfo);
        assertEquals(200, response.getStatus());
        Feed feed = (Feed) response.getEntity();

        List<Entry> entries = feed.getEntries();
        assertEquals(0, entries.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetJobHistory() throws Exception {
        Set<String> names = new HashSet<String>();
        names.add("foo");
        when(jobService.listJobNames()).thenReturn(names);
        when(jobInfoRepository.findByNameAndTimeRange(anyString(), any(Date.class), any(Date.class))).
                thenReturn(createJobs(5, "foo"));

        Response response = jobInfoResource.getJobsHistory(5);
        assertEquals(200, response.getStatus());
        Map<String, JobInfoRepresentation> history = (Map<String, JobInfoRepresentation>) response.getEntity();
        assertEquals(1, history.size());
    }

    private List<JobInfo> createJobs(int number, String name) {
        List<JobInfo> jobs = new ArrayList<JobInfo>();
        for (int i = 0; i < number; i++) {
            jobs.add(new JobInfo(new BasicDBObject().append(JobInfoProperty.ID.val(), String.valueOf(i)).
                    append(JobInfoProperty.NAME.val(), name)));
        }
        return jobs;
    }
}

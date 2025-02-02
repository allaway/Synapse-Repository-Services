package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class TableCSVDownloadWorkerTest {

	@Mock
	private TableQueryManager mockTableQueryManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private Clock mockClock;
	@Mock
	private TableExceptionTranslator mockTableExceptionTranslator;
	@Mock
	private AsyncJobProgressCallback mockJobProgressCallback;
	@Captor
	private ArgumentCaptor<LocalFileUploadRequest> fileUploadCaptor;

	@InjectMocks
	private TableCSVDownloadWorker worker;

	Long userId;
	UserInfo userInfo;
	DownloadFromTableRequest request;
	AsynchronousJobStatus status;
	String jobId;
	Message message;

	DownloadFromTableResult results;

	@BeforeEach
	public void before() throws Exception {
		userId = 987L;
		userInfo = new UserInfo(false);
		userInfo.setId(userId);

		request = new DownloadFromTableRequest();
		request.setSql("select * from syn123");

		jobId = "1";
		status = new AsynchronousJobStatus();
		status.setJobId(jobId);
		status.setRequestBody(request);
		status.setStartedByUserId(userId);

		message = new Message();
		message.setBody(jobId);

		results = new DownloadFromTableResult();

		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenReturn(new QueryResultBundle().setQueryCount(100L));
	}

	@Test
	public void testBasicQuery() throws Exception {
		when(mockTableQueryManager.runQueryDownloadAsStream(any(), any(), any(), any())).thenReturn(results);
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId("8888"));
		
		// call under test
		DownloadFromTableResult response = worker.run(jobId, userInfo, request, mockJobProgressCallback);
		
		assertEquals(results, response);
		
		verify(mockFileHandleManager).uploadLocalFile(fileUploadCaptor.capture());
		LocalFileUploadRequest request = fileUploadCaptor.getValue();
		assertNotNull(request);
		assertEquals(userInfo.getId().toString(), request.getUserId());
		assertEquals("text/csv", request.getContentType());
		assertEquals(null, request.getFileName());
	}

	@Test
	public void testTableUnavailableException() throws Exception {
		// table not available
		when(mockTableQueryManager.runQueryDownloadAsStream(any(), any(),any(), any())).thenThrow(new TableUnavailableException(new TableStatus()));
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobProgressCallback);
		});
	}

	@Test
	public void testLockUnavilableExceptionException() throws Exception {
		// table not available
		when(mockTableQueryManager.runQueryDownloadAsStream(any(), any(),any(), any())).thenThrow(new LockUnavilableException());
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobProgressCallback);
		});
	}

	@Test
	public void testTableFailedExceptionException() throws Exception {
		TableFailedException exception = new TableFailedException(new TableStatus());
		// table not available
		when(mockTableQueryManager.runQueryDownloadAsStream(any(), any(),any(), any())).thenThrow(exception);
		
		TableFailedException result = assertThrows(TableFailedException.class, () -> {			
			// call under test
			worker.run(jobId, userInfo, request, mockJobProgressCallback);
		});
		
		assertEquals(result, exception);
	}

	@Test
	public void testUnknownException() throws Exception {
		RuntimeException translatedException = new RuntimeException("translated");
		when(mockTableExceptionTranslator.translateException(any())).thenReturn(translatedException);
		RuntimeException error = new RuntimeException("Bad stuff happened");
		// table not available
		when(mockTableQueryManager.runQueryDownloadAsStream(any(), any(),any(), any())).thenThrow(error);
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobProgressCallback);
		});
		
		assertEquals(translatedException, result);
		// the exception should be translated.
		verify(mockTableExceptionTranslator).translateException(error);
	}
}

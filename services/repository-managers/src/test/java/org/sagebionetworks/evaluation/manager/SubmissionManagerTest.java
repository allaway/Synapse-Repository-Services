package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.test.util.ReflectionTestUtils;

public class SubmissionManagerTest {
		
	private SubmissionManager submissionManager;	
	private Evaluation eval;
	private Participant part;
	private Submission sub;
	private Submission sub2;
	private Submission subWithId;
	private Submission sub2WithId;
	private SubmissionStatus subStatus;
	
	private IdGenerator mockIdGenerator;
	private SubmissionDAO mockSubmissionDAO;
	private SubmissionStatusDAO mockSubmissionStatusDAO;
	private SubmissionFileHandleDAO mockSubmissionFileHandleDAO;
	private EvaluationManager mockEvaluationManager;
	private ParticipantManager mockParticipantManager;
	private EntityManager mockEntityManager;
	private NodeManager mockNodeManager;
	private FileHandleManager mockFileHandleManager;
	private EvaluationPermissionsManager mockEvalPermissionsManager;
	private Node mockNode;
	private Folder folder;
	private EntityBundle bundle;
	
    private FileHandle fileHandle1;
    private FileHandle fileHandle2;
	
	private static final String EVAL_ID = "12";
	private static final String OWNER_ID = "34";
	private static final String USER_ID = "56";
	private static final String SUB_ID = "78";
	private static final String SUB2_ID = "87";
	private static final String ENTITY_ID = "90";
	private static final String ENTITY2_ID = "99";
	private static final String ETAG = "etag";	
	private static final String HANDLE_ID_2 = "handle2";
	private static final String HANDLE_ID_1 = "handle1";
	private static final String TEST_URL = "http://www.foo.com/bar";
	
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
	
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException, MalformedURLException {
		// User Info
    	ownerInfo = UserInfoUtils.createValidUserInfo(false);
    	ownerInfo.getIndividualGroup().setId(OWNER_ID);
    	userInfo = UserInfoUtils.createValidUserInfo(false);
    	userInfo.getIndividualGroup().setId(USER_ID);
    	
    	// FileHandles
		List<FileHandle> handles = new ArrayList<FileHandle>();
		List<String> handleIds = new ArrayList<String>();
		fileHandle1 = new PreviewFileHandle();
		fileHandle1.setId(HANDLE_ID_1);
		handles.add(fileHandle1);
		handleIds.add(HANDLE_ID_1);
		fileHandle2 = new PreviewFileHandle();
		fileHandle2.setId(HANDLE_ID_2);
		handles.add(fileHandle2);
		handleIds.add(HANDLE_ID_2);
    	
    	// Objects
		eval = new Evaluation();
		eval.setName("compName");
		eval.setId(EVAL_ID);
		eval.setOwnerId(OWNER_ID);
        eval.setContentSource(KeyFactory.SYN_ROOT_ID);
        eval.setStatus(EvaluationStatus.OPEN);
        eval.setCreatedOn(new Date());
        eval.setEtag("compEtag");
        
        part = new Participant();
        part.setEvaluationId(EVAL_ID);
        part.setCreatedOn(new Date());
        part.setUserId(USER_ID);
        
        sub = new Submission();
        sub.setEvaluationId(EVAL_ID);
        sub.setCreatedOn(new Date());
        sub.setEntityId(ENTITY_ID);
        sub.setName("subName");
        sub.setUserId(USER_ID);
        sub.setSubmitterAlias("Team Awesome!");
        sub.setVersionNumber(0L);
        
        sub2 = new Submission();
        sub2.setEvaluationId(EVAL_ID);
        sub2.setCreatedOn(new Date());
        sub2.setEntityId(ENTITY2_ID);
        sub2.setName("subName");
        sub2.setUserId(OWNER_ID);
        sub2.setSubmitterAlias("Team Even More Awesome!");
        sub2.setVersionNumber(0L);
        
        subWithId = new Submission();
        subWithId.setId(SUB_ID);
        subWithId.setEvaluationId(EVAL_ID);
        subWithId.setCreatedOn(new Date());
        subWithId.setEntityId(ENTITY_ID);
        subWithId.setName("subName");
        subWithId.setUserId(USER_ID);
        subWithId.setSubmitterAlias("Team Awesome!");
        subWithId.setVersionNumber(0L);
        
        sub2WithId = new Submission();
        sub2WithId.setId(SUB2_ID);
        sub2WithId.setEvaluationId(EVAL_ID);
        sub2WithId.setCreatedOn(new Date());
        sub2WithId.setEntityId(ENTITY2_ID);
        sub2WithId.setName("subName");
        sub2WithId.setUserId(OWNER_ID);
        sub2WithId.setSubmitterAlias("Team Even More Awesome!");
        sub2WithId.setVersionNumber(0L);
        
        subStatus = new SubmissionStatus();
        subStatus.setEtag("subEtag");
        subStatus.setId(SUB_ID);
        subStatus.setModifiedOn(new Date());
        subStatus.setScore(0.0);
        subStatus.setStatus(SubmissionStatusEnum.OPEN);
        
        folder = new Folder();
        bundle = new EntityBundle();
        bundle.setEntity(folder);
        bundle.setFileHandles(handles);
		
    	// Mocks
        mockIdGenerator = mock(IdGenerator.class);
    	mockSubmissionDAO = mock(SubmissionDAO.class);
    	mockSubmissionStatusDAO = mock(SubmissionStatusDAO.class);
    	mockSubmissionFileHandleDAO = mock(SubmissionFileHandleDAO.class);
    	mockEvaluationManager = mock(EvaluationManager.class);
    	mockParticipantManager = mock(ParticipantManager.class);
    	mockEntityManager = mock(EntityManager.class);
    	mockNodeManager = mock(NodeManager.class, RETURNS_DEEP_STUBS);
    	mockNode = mock(Node.class);
      	mockFileHandleManager = mock(FileHandleManager.class);
      	mockEvalPermissionsManager = mock(EvaluationPermissionsManager.class);

    	when(mockIdGenerator.generateNewId()).thenReturn(Long.parseLong(SUB_ID));
    	when(mockParticipantManager.getParticipant(eq(USER_ID), eq(EVAL_ID))).thenReturn(part);
    	when(mockEvaluationManager.getEvaluation(eq(EVAL_ID))).thenReturn(eval);
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.get(eq(SUB2_ID))).thenReturn(sub2WithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockSubmissionStatusDAO.get(eq(SUB_ID))).thenReturn(subStatus);
    	when(mockNode.getNodeType()).thenReturn(EntityType.values()[0].toString());
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.get(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);    	
    	when(mockNodeManager.get(eq(userInfo), eq(ENTITY2_ID))).thenThrow(new UnauthorizedException());
    	when(mockNodeManager.getNodeForVersionNumber(eq(userInfo), eq(ENTITY_ID), anyLong())).thenReturn(mockNode);
    	when(mockNodeManager.getNodeForVersionNumber(eq(userInfo), eq(ENTITY2_ID), anyLong())).thenThrow(new UnauthorizedException());
    	when(mockEntityManager.getEntityForVersion(any(UserInfo.class), anyString(), anyLong(), any(Class.class))).thenReturn(folder);
    	when(mockSubmissionFileHandleDAO.getAllBySubmission(eq(SUB_ID))).thenReturn(handleIds);
    	when(mockFileHandleManager.getRedirectURLForFileHandle(eq(HANDLE_ID_1))).thenReturn(new URL(TEST_URL));
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.PARTICIPATE))).thenReturn(true);
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(true);

    	// Submission Manager
    	submissionManager = new SubmissionManagerImpl(mockIdGenerator, mockSubmissionDAO, 
    			mockSubmissionStatusDAO, mockSubmissionFileHandleDAO, mockEvaluationManager, 
    			mockParticipantManager, mockEntityManager, mockNodeManager, 
    			mockFileHandleManager);
    	ReflectionTestUtils.setField(submissionManager, "evaluationPermissionsManager", mockEvalPermissionsManager);
    }
	
	@Test
	public void testCRUDAsAdmin() throws Exception {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		submissionManager.createSubmission(userInfo, sub, ETAG, bundle);
		submissionManager.getSubmission(userInfo, SUB_ID);
		submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
		submissionManager.deleteSubmission(ownerInfo, SUB_ID);
		verify(mockSubmissionDAO).create(any(Submission.class));
		verify(mockSubmissionDAO).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockSubmissionStatusDAO).update(any(SubmissionStatus.class));
		verify(mockSubmissionFileHandleDAO).create(eq(SUB_ID), eq(fileHandle1.getId()));
		verify(mockSubmissionFileHandleDAO).create(eq(SUB_ID), eq(fileHandle2.getId()));		
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCAsUser_NotAuthorized() throws Exception {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		when(mockEvalPermissionsManager.hasAccess(
				eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.PARTICIPATE))).thenReturn(false);
		submissionManager.createSubmission(userInfo, sub, ETAG, bundle);		
	}
	
	@Test
	public void testCRUDAsUser() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		submissionManager.createSubmission(userInfo, sub, ETAG, bundle);
		submissionManager.getSubmission(userInfo, SUB_ID);
		try {
			submissionManager.updateSubmissionStatus(userInfo, subStatus);
			fail();
		} catch (UnauthorizedException e) {
			//expected
		}
		try {
			submissionManager.deleteSubmission(userInfo, SUB_ID);
			fail();
		} catch (UnauthorizedException e) {
			//expected
		}
		verify(mockSubmissionDAO).create(any(Submission.class));
		verify(mockSubmissionDAO, never()).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockSubmissionStatusDAO, never()).update(any(SubmissionStatus.class));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUnauthorizedGet() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		submissionManager.getSubmission(userInfo, SUB2_ID);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUnauthorizedEntity() throws NotFoundException, DatastoreException, JSONObjectAdapterException {		
		// user should not have access to sub2
		submissionManager.createSubmission(userInfo, sub2, ETAG, bundle);		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidScore() throws Exception {
		submissionManager.createSubmission(userInfo, sub, ETAG, bundle);
		submissionManager.getSubmission(userInfo, SUB_ID);
		subStatus.setScore(1.1);
		submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
	}
		
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidEtag() throws Exception {
		submissionManager.createSubmission(userInfo, sub, ETAG + "modified", bundle);
	}
	
	@Test
	public void testGetAllSubmissions() throws DatastoreException, UnauthorizedException, NotFoundException {
		SubmissionStatusEnum statusEnum = SubmissionStatusEnum.CLOSED;
		submissionManager.getAllSubmissions(ownerInfo, EVAL_ID, null, 10, 0);
		submissionManager.getAllSubmissions(ownerInfo, EVAL_ID, statusEnum, 10, 0);
		verify(mockSubmissionDAO).getAllByEvaluation(eq(EVAL_ID), anyLong(), anyLong());
		verify(mockSubmissionDAO).getAllByEvaluationAndStatus(eq(EVAL_ID), eq(statusEnum), eq(10L), eq(0L));
	}
	
	@Test(expected = UnauthorizedException.class)
	public void testGetAllSubmissionsUnauthorized() throws DatastoreException, UnauthorizedException, NotFoundException {
		submissionManager.getAllSubmissions(ownerInfo, USER_ID, null, 10, 0);
	}
	
	@Test
	public void testGetAllSubmissionsByUser() throws DatastoreException, NotFoundException {
		submissionManager.getAllSubmissionsByUser(USER_ID, 10, 0);
		verify(mockSubmissionDAO).getAllByUser(eq(USER_ID), eq(10L), eq(0L));
	}
	
	@Test
	public void testGetSubmissionCount() throws DatastoreException, NotFoundException {
		submissionManager.getSubmissionCount(EVAL_ID);
		verify(mockSubmissionDAO).getCountByEvaluation(eq(EVAL_ID));
	}
	
	@Test
	public void testGetRedirectURLForFileHandle()
			throws DatastoreException, NotFoundException {
		URL url = submissionManager.getRedirectURLForFileHandle(ownerInfo, SUB_ID, HANDLE_ID_1);
		assertEquals(TEST_URL, url.toString());
		verify(mockSubmissionFileHandleDAO).getAllBySubmission(eq(SUB_ID));
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(HANDLE_ID_1));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetRedirectURLForFileHandleUnauthorized()
			throws DatastoreException, NotFoundException {
		submissionManager.getRedirectURLForFileHandle(userInfo, SUB_ID, HANDLE_ID_1);
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetRedirectURLForFileHandleNotFound() 
			throws DatastoreException, NotFoundException {
		// HANDLE_ID_1 is not contained in this Submission
		submissionManager.getRedirectURLForFileHandle(ownerInfo, SUB2_ID, HANDLE_ID_1);
	}

}

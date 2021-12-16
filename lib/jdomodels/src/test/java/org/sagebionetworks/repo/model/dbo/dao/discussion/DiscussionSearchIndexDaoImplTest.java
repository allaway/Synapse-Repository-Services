package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBODiscussionSearchIndexRecord;
import org.sagebionetworks.repo.model.discussion.Match;
import org.sagebionetworks.repo.model.helper.NodeDaoObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DiscussionSearchIndexDaoImplTest {

	@Autowired
	private NodeDaoObjectHelper nodeHelper;
		
	@Autowired
	private ForumDAO forumDao;
	
	@Autowired
	private DiscussionThreadDAO threadDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private DiscussionSearchIndexDaoImpl dao;
	
	private Long forumId;
	private Long threadId;

	@BeforeEach
	public void before() {
		
		String projectId = nodeHelper.create(node -> {
			node.setNodeType(EntityType.project);
		}).getId();
		
		forumId = Long.valueOf(forumDao.createForum(projectId).getId());
		threadId = Long.valueOf(threadDao.createThread(forumId.toString(), idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID).toString(), "title", "some_key", BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()).getId());
	}
	
	@AfterEach
	public void after() {
		nodeHelper.truncateAll();
	}
	
	@Test
	public void testCreateRecordForThread() {
				
		DBODiscussionSearchIndexRecord expected = createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, "search content");
		
		// Call under test
		dao.createRecordForThread(forumId, threadId, expected.getSearchContent());
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
		
	}
	
	@Test
	public void testCreateRecordForThreadExisting() {
		
		dao.createRecordForThread(forumId, threadId, "search content");
		
		DBODiscussionSearchIndexRecord expected = createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, "search content updated");
		
		// Call under test
		dao.createRecordForThread(forumId, threadId, expected.getSearchContent());
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
		
	}
	
	@Test
	public void testCreateRecordForThreadWithNoForum() {

		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createRecordForThread(null, threadId, searchContent);
		}).getMessage();
		
		assertEquals("The forumId is required.", message);
		
	}
	
	@Test
	public void testCreateRecordForThreadWithNoThread() {

		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createRecordForThread(forumId, null, searchContent);
		}).getMessage();
		
		assertEquals("The threadId is required.", message);
		
	}
	
	@Test
	public void testCreateRecordForThreadWithNoSearchContent() {

		String searchContent = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createRecordForThread(forumId, threadId, searchContent);
		}).getMessage();
		
		assertEquals("The searchContent is required.", message);
		
	}
	
	@Test
	public void testCreateRecordForReply() {
		
		Long replyId = 123L;
				
		DBODiscussionSearchIndexRecord expected = createRecord(replyId, "search content");
		
		// Call under test
		dao.createRecordForReply(forumId, threadId, replyId, expected.getSearchContent());
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
		
	}
	
	@Test
	public void testCreateRecordForReplyExisting() {
		
		Long replyId = 123L;
		
		dao.createRecordForReply(forumId, threadId, replyId, "search content");
		
		DBODiscussionSearchIndexRecord expected = createRecord(replyId, "search content updated");
		
		// Call under test
		dao.createRecordForReply(forumId, threadId, replyId, expected.getSearchContent());
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
		
	}
	
	@Test
	public void testCreateRecordForReplyWithNoForum() {
		
		Long replyId = 123L;
				
		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createRecordForReply(null, threadId, replyId, searchContent);
		}).getMessage();
		
		assertEquals("The forumId is required.", message);
	}
	
	@Test
	public void testCreateRecordForReplyWithNoThread() {
		
		Long replyId = 123L;
				
		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createRecordForReply(forumId, null, replyId, searchContent);
		}).getMessage();
		
		assertEquals("The threadId is required.", message);
	}
	
	@Test
	public void testCreateRecordForReplyWithNoReply() {
		
		Long replyId = null;
				
		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createRecordForReply(forumId, threadId, replyId, searchContent);
		}).getMessage();
		
		assertEquals("The replyId is required.", message);
	}
	
	@Test
	public void testCreateRecordForReplyWithUnexpectedReply() {
		
		Long replyId = DBODiscussionSearchIndexRecord.NO_REPLY_ID;
				
		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createRecordForReply(forumId, threadId, replyId, searchContent);
		}).getMessage();
		
		assertEquals("Unexpected replyId: " + replyId, message);
	}	
	
	@Test
	public void testCreateRecordForReplyWithNoSearchContent() {
		
		Long replyId = 123L;
				
		String searchContent = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createRecordForReply(forumId, threadId, replyId, searchContent);
		}).getMessage();
		
		assertEquals("The searchContent is required.", message);
	}
	
	
	@Test
	public void deleteByForum() {
		Long replyId = 123L;
		
		dao.createRecordForThread(forumId, threadId, "search content");
		dao.createRecordForReply(forumId, threadId, replyId, "search content");
		
		// Call under test
		dao.deleteByForumId(forumId);
		
		assertEquals(Collections.emptyList(), dao.listRecords(forumId));
	}
	
	@Test
	public void deleteByForumNoForum() {		
		String message = assertThrows(IllegalArgumentException.class, () -> { 			
			// Call under test
			dao.deleteByForumId(null);
		}).getMessage();
		
		assertEquals("The forumId is required.", message);
	}
	
	@Test
	public void deleteByThread() {
		Long replyId = 123L;
		
		dao.createRecordForThread(forumId, threadId, "search content");
		dao.createRecordForReply(forumId, threadId, replyId, "search content");
		
		// Call under test
		dao.deleteByThreadId(threadId);
		
		assertEquals(Collections.emptyList(), dao.listRecords(forumId));
	}
	
	@Test
	public void deleteByThreadNoThread() {
		
		String message = assertThrows(IllegalArgumentException.class, () -> { 			
			// Call under test
			dao.deleteByThreadId(null);
		}).getMessage();
		
		assertEquals("The threadId is required.", message);
	}
	
	@Test
	public void deleteByReply() {
		Long replyId = 123L;
		
		dao.createRecordForThread(forumId, threadId, "search content");
		dao.createRecordForReply(forumId, threadId, replyId, "search content");
		
		DBODiscussionSearchIndexRecord expected = createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, "search content");
		
		// Call under test
		dao.deleteByReplyId(replyId);
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
	}
	
	@Test
	public void deleteByReplyNoReply() {
		
		String message = assertThrows(IllegalArgumentException.class, () -> { 			
			// Call under test
			dao.deleteByReplyId(null);
		}).getMessage();
		
		assertEquals("The replyId is required.", message);
	}
	
	@Test
	public void testSearch() {
		String searchString = "search content";
		
		Long replyId = 123L;
		
		dao.createRecordForThread(forumId, threadId, "search content thread");
		dao.createRecordForReply(forumId, threadId, replyId, "content reply");
		dao.createRecordForReply(forumId, threadId, replyId + 1, "non-matching reply");
		
		List<Match> expected = Arrays.asList(
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(null),
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(replyId.toString())
		);
		
		// Call under test
		List<Match> result = dao.search(forumId, searchString, 10, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchMultiplePages() {
		
		long limit = 2;
		long offset = 0;
		
		String searchString = "search content";
		
		Long replyId = 123L;
		Long anotherReplyId = 456L;
		
		dao.createRecordForThread(forumId, threadId, "search content content thread");
		dao.createRecordForReply(forumId, threadId, replyId, "content content reply");
		dao.createRecordForReply(forumId, threadId, anotherReplyId, "content reply");
		
		List<Match> expectedFirstPage = Arrays.asList(
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(null),
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(replyId.toString())
		);
		
		// Call under test
		List<Match> result = dao.search(forumId, searchString, limit, offset);
		
		assertEquals(expectedFirstPage, result);
		
		List<Match> expectedSecondPage = Arrays.asList(
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(anotherReplyId.toString())
		);
		
		// Call under test
		result = dao.search(forumId, searchString, limit, offset + limit);
		
		assertEquals(expectedSecondPage, result);
	}
	
	@Test
	public void testSearchNoForum() {
		String searchString = "search string";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.search(null, searchString, 10, 0);
		}).getMessage();
		
		assertEquals("The forumId is required.", message);
		
	}
	
	@Test
	public void testSearchNoSearchString() {
		String searchString = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.search(forumId, searchString, 10, 0);
		}).getMessage();
		
		assertEquals("The searchString is required.", message);
		
	}
	
	private DBODiscussionSearchIndexRecord createRecord(Long replyId, String searchContent) {
		DBODiscussionSearchIndexRecord record = new DBODiscussionSearchIndexRecord();
		record.setForumId(forumId);
		record.setThreadId(threadId);
		record.setReplyId(replyId);
		record.setSearchContent(searchContent);
		return record;
	}

}

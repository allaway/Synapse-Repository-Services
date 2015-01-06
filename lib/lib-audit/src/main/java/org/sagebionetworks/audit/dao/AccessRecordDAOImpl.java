package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.audit.utils.AccessRecordUtils;
import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.audit.utils.SimpleRecordWorker;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;

/**
 * This implementation of the AccessRecordDAO uses S3 as the permanent
 * datastore.
 * 
 * @author John
 * 
 */
public class AccessRecordDAOImpl implements AccessRecordDAO {
	
	/**
	 * This is the schema. If it changes we will not be able to read old data.
	 */
	private final static String[] HEADERS = new String[]{"returnObjectId", "elapseMS","timestamp","via","host","threadId","userAgent","queryString","sessionId","xForwardedFor","requestURL","userId","origin", "date","method","vmId","instance","stack","success", "responseStatus"};

	@Autowired
	private AmazonS3Client s3Client;

	/**
	 * Injected via Spring
	 */
	private String auditRecordBucketName;
	/**
	 * Injected via Spring
	 */
	int stackInstanceNumber;
	String stackInstancePrefixString;
	private SimpleRecordWorker<AccessRecord> worker;

	/**
	 * Injected via Spring
	 * 
	 * @param auditRecordBucketName
	 */
	public void setAuditRecordBucketName(String auditRecordBucketName) {
		this.auditRecordBucketName = auditRecordBucketName;
	}

	/**
	 * Injected via Spring
	 * 
	 * @param stackInstanceNumber
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
		this.stackInstancePrefixString = KeyGeneratorUtil.getInstancePrefix(stackInstanceNumber);
	}

	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize() {
		if (auditRecordBucketName == null)
			throw new IllegalArgumentException(
					"bucketName has not been set and cannot be null");
		// Create the bucket if it does not exist
		s3Client.createBucket(auditRecordBucketName);
		worker = new SimpleRecordWorker<AccessRecord>(s3Client, stackInstanceNumber, 
				auditRecordBucketName, AccessRecord.class, HEADERS);
	}
	
	@Override
	public String saveBatch(List<AccessRecord> batch, boolean rolling) throws IOException {
		// Save with the current timesamp
		return saveBatch(batch, System.currentTimeMillis(), rolling);
	}

	@Override
	public String saveBatch(List<AccessRecord> batch, long timestamp, boolean rolling) throws IOException {
		if(batch == null) throw new IllegalArgumentException("Batch cannot be null");
		// Order the batch by timestamp
		AccessRecordUtils.sortByTimestamp(batch);
		return worker.write(batch, timestamp, rolling);
	}

	@Override
	public List<AccessRecord> getBatch(String key) throws IOException {
		return worker.read(key);
	}

	@Override
	public void deleteBactch(String key) {
		worker.delete(key);
	}
	
	@Override
	public void deleteAllStackInstanceBatches() {
		worker.deleteAllStackInstanceBatches(this.stackInstancePrefixString);
	}
	
	@Override
	public ObjectListing listBatchKeys(String marker) {
		return worker.listBatchKeys(stackInstancePrefixString, marker);
	}

}

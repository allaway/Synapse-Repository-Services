package org.sagebionetworks.ses.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.ses.SESNotificationManager;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;
import org.sagebionetworks.repo.model.ses.SESNotificationUtils;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class SESNotificationWorker implements MessageDrivenRunner {

	private static final Logger LOG = LogManager.getLogger(SESNotificationWorker.class);

	private SESNotificationManager notificationManager;
	private WorkerLogger workerLogger;

	@Autowired
	public SESNotificationWorker(SESNotificationManager notificationManager, WorkerLogger workerLogger) {
		this.notificationManager = notificationManager;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		String notificationBody = message.getBody();
		
		try {
			SESJsonNotification notification = SESNotificationUtils.parseNotification(notificationBody);
			notification.setNotificationBody(notificationBody);
			
			notificationManager.processNotification(notification);
			
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);

			boolean willRetry = false;
			
			// Sends a fail metric for cloud watch
			workerLogger.logWorkerFailure(SESNotificationWorker.class.getName(), e, willRetry);
		}

	}

}

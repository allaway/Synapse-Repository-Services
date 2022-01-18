package org.sagebionetworks.download.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class AddToDownloadListWorker implements AsyncJobRunner<AddToDownloadListRequest, AddToDownloadListResponse> {

	private DownloadListManager downloadListManager;

	@Autowired
	public AddToDownloadListWorker(DownloadListManager downloadListManager) {
		this.downloadListManager = downloadListManager;
	}
	
	@Override
	public Class<AddToDownloadListRequest> getRequestType() {
		return AddToDownloadListRequest.class;
	}
	
	@Override
	public Class<AddToDownloadListResponse> getResponseType() {
		return AddToDownloadListResponse.class;
	}

	@Override
	public AddToDownloadListResponse run(ProgressCallback progressCallback, String jobId, UserInfo user, AddToDownloadListRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		return downloadListManager.addToDownloadList(progressCallback, user, request);
	}

}

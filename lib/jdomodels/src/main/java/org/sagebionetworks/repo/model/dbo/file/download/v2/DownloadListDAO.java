package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.List;

import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.Sort;

public interface DownloadListDAO {

	/**
	 * Add a batch of files to a user's download list.
	 * 
	 * @param userId     The id of the user.
	 * @param batchToAdd The batch of files to add.
	 * @return The number of files that were actually added.
	 */
	long addBatchOfFilesToDownloadList(Long userId, List<DownloadListItem> batchToAdd);

	/**
	 * 
	 * @param userId        The id of the user.
	 * @param batchToRemove The batch of files to remove.
	 * @return The number of files that were actually removed.
	 */
	long removeBatchOfFilesFromDownloadList(Long userId, List<DownloadListItem> batchToRemove);

	/**
	 * Clear all files from the user's download list.
	 * 
	 * @param userId
	 */
	void clearDownloadList(Long userId);

	/**
	 * Get a single page of files from a user's download list that are available for
	 * download.
	 * 
	 * @param accessCallback Callback used to determine which entities on the user's
	 *                       download list that the user can download.
	 * @param userId
	 * @param sort
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<DownloadListItemResult> getFilesAvailableToDownloadFromDownloadList(EntityAccessCallback accessCallback,
			Long userId, List<Sort> sort, Long limit, Long offset);

	/**
	 * Get the DBODownloadList for the given user.
	 * 
	 * @param userId
	 * @return
	 */
	DBODownloadList getDBODownloadList(Long userId);

	/**
	 * 
	 * @param userId
	 * @return
	 */
	List<DBODownloadListItem> getDBODownloadListItems(Long userId);
	
	/**
	 * Get DownloadListItemResult for the given user and item.
	 * @param userId
	 * @param entityIds
	 * @return
	 */
	DownloadListItemResult getDownloadListItem(Long userId, DownloadListItem item);
	
	/**
	 * Get the DownloadListItemResult for the given user and each item.
	 * @param userId
	 * @param item
	 * @return
	 */
	List<DownloadListItemResult> getDownloadListItems(Long userId, DownloadListItem...item);

	/**
	 * Clear all download data for all users.
	 */
	void truncateAllData();

}

package org.sagebionetworks.repo.manager.dataaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.joda.time.Instant;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.BasicAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionEvent;
import org.sagebionetworks.repo.model.dataaccess.ManagedACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfo;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionReviewerFilterType;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchResponse;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchResult;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchSort;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSortField;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Note that the "dataAccessSubmissionManager" qualifier is needed so that it does not conflict with the existing SubmissionManager for the evaluations:
// Spring by default assigns the name of the class to each autowired bean, so we need a unique name in the same context in order for it to be injected automatically
@Service("dataAccessSubmissionManager")
public class SubmissionManagerImpl implements SubmissionManager{

	private AccessRequirementDAO accessRequirementDao;
	private RequestManager requestManager;
	private ResearchProjectDAO researchProjectDao;
	private SubmissionDAO submissionDao;
	private AccessApprovalDAO accessApprovalDao;
	private SubscriptionDAO subscriptionDao;
	private TransactionalMessenger transactionalMessenger;
	private AccessApprovalManager accessAprovalManager;
	private DataAccessAuthorizationManager authorizationManager;
	
	@Autowired
	public SubmissionManagerImpl(AccessRequirementDAO accessRequirementDao, RequestManager requestManager,
			ResearchProjectDAO researchProjectDao, SubmissionDAO submissionDao, AccessApprovalDAO accessApprovalDao,
			SubscriptionDAO subscriptionDao, TransactionalMessenger transactionalMessenger, AccessApprovalManager accessAprovalManager,
			DataAccessAuthorizationManager authorizationManager) {
		this.accessRequirementDao = accessRequirementDao;
		this.requestManager = requestManager;
		this.researchProjectDao = researchProjectDao;
		this.submissionDao = submissionDao;
		this.accessApprovalDao = accessApprovalDao;
		this.subscriptionDao = subscriptionDao;
		this.transactionalMessenger = transactionalMessenger;
		this.accessAprovalManager = accessAprovalManager;
		this.authorizationManager = authorizationManager;
	}

	@WriteTransaction
	@Override
	public SubmissionStatus create(UserInfo userInfo, CreateSubmissionRequest createSubmissionRequest) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(createSubmissionRequest, "createSubmissionRequest");
		ValidateArgument.required(createSubmissionRequest.getRequestId(), "requestId");
		ValidateArgument.required(createSubmissionRequest.getRequestEtag(), "etag");
		ValidateArgument.required(createSubmissionRequest.getSubjectId(), "subjectId");
		ValidateArgument.required(createSubmissionRequest.getSubjectType(), "subjectType");
		RequestInterface request = requestManager.getRequestForSubmission(createSubmissionRequest.getRequestId());
		ValidateArgument.requirement(createSubmissionRequest.getRequestEtag().equals(request.getEtag()), "Etag does not match.");

		Submission submissionToCreate = new Submission();
		submissionToCreate.setRequestId(request.getId());
		submissionToCreate.setResearchProjectSnapshot(researchProjectDao.get(request.getResearchProjectId()));
		submissionToCreate.setSubjectId(createSubmissionRequest.getSubjectId());
		submissionToCreate.setSubjectType(createSubmissionRequest.getSubjectType());

		validateRequestBasedOnRequirements(userInfo, request, submissionToCreate);
		prepareCreationFields(userInfo, submissionToCreate);
		SubmissionStatus status = submissionDao.createSubmission(submissionToCreate);
		subscriptionDao.create(userInfo.getId().toString(), status.getSubmissionId(), SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS);

		MessageToSend changeMessage = new MessageToSend()
				.withUserId(userInfo.getId())
				.withObjectType(ObjectType.DATA_ACCESS_SUBMISSION)
				.withObjectId(status.getSubmissionId())
				.withChangeType(ChangeType.CREATE);
		
		transactionalMessenger.sendMessageAfterCommit(changeMessage);

		sendLocalEventAfterCommit(status.getSubmissionId());

		
		return status;
	}
	
	/**
	 * This will send a message to a local topic only. The event will not propagate
	 * to staging. A local event listener will then send a notification email to
	 * users assigned to review this submission.
	 * 
	 * @param submissionId
	 */
	private void sendLocalEventAfterCommit(String submissionId) {
		transactionalMessenger.publishMessageAfterCommit(new DataAccessSubmissionEvent().setObjectId(submissionId)
				.setObjectType(ObjectType.DATA_ACCESS_SUBMISSION_EVENT).setTimestamp(Instant.now().toDate()));
	}

	/**
	 * Validates the given request based on the existing requirements.
	 * If the request is valid, populates the fields for submissionToCreate.
	 * 
	 * @param userInfo
	 * @param request
	 * @param submissionToCreate
	 */
	public void validateRequestBasedOnRequirements(UserInfo userInfo, RequestInterface request,
			Submission submissionToCreate) {

		ValidateArgument.required(request.getAccessRequirementId(), "accessRequirementId");
		submissionToCreate.setAccessRequirementId(request.getAccessRequirementId());
		ValidateArgument.requirement(!submissionDao.hasSubmissionWithState(
				userInfo.getId().toString(), request.getAccessRequirementId(), SubmissionState.SUBMITTED),
				"A submission has been created. It has to be reviewed or cancelled before another submission can be created.");

		AccessRequirement ar = accessRequirementDao.get(request.getAccessRequirementId());
		ValidateArgument.requirement(ar instanceof ManagedACTAccessRequirement,
				"A Submission can only be created for an ManagedACTAccessRequirement.");
		submissionToCreate.setAccessRequirementVersion(ar.getVersionNumber());

		// validate based on the access requirement
		ManagedACTAccessRequirement actAR = (ManagedACTAccessRequirement) ar;
		if (actAR.getIsDUCRequired()) {
			ValidateArgument.requirement(request.getDucFileHandleId()!= null,
					"You must provide a Data Use Certification document.");
			submissionToCreate.setDucFileHandleId(request.getDucFileHandleId());
		}
		if (actAR.getIsIRBApprovalRequired()) {
			ValidateArgument.requirement(request.getIrbFileHandleId()!= null,
					"You must provide an Institutional Review Board approval document.");
			submissionToCreate.setIrbFileHandleId(request.getIrbFileHandleId());
		}
		if (actAR.getAreOtherAttachmentsRequired()) {
			ValidateArgument.requirement(request.getAttachments()!= null && !request.getAttachments().isEmpty(),
					"You must provide the required attachment(s).");
			submissionToCreate.setAttachments(request.getAttachments());
		}
		ValidateArgument.requirement(request.getAccessorChanges() != null && !request.getAccessorChanges().isEmpty(),
				"Must provide at least one accessor.");

		Set<String> accessors = new HashSet<String>();
		Set<String> accessorsWillHaveAccess = new HashSet<String>();
		Set<String> accessorsAlreadyHaveAccess = new HashSet<String>();
		for (AccessorChange ac : request.getAccessorChanges()) {
			ValidateArgument.requirement(!accessors.contains(ac.getUserId()),
					"Accessor "+ac.getUserId()+" is listed more than one.");
			accessors.add(ac.getUserId());
			switch (ac.getType()) {
				case GAIN_ACCESS:
					accessorsWillHaveAccess.add(ac.getUserId());
					break;
				case RENEW_ACCESS:
					accessorsWillHaveAccess.add(ac.getUserId());
					accessorsAlreadyHaveAccess.add(ac.getUserId());
					break;
				case REVOKE_ACCESS:
					accessorsAlreadyHaveAccess.add(ac.getUserId());
					break;
			}
		}

		accessAprovalManager.validateHasAccessorRequirement(actAR, accessorsWillHaveAccess);

		if (!accessorsAlreadyHaveAccess.isEmpty()) {
			ValidateArgument.requirement(accessApprovalDao.hasApprovalsSubmittedBy(
					accessorsAlreadyHaveAccess, userInfo.getId().toString(), request.getAccessRequirementId()),
					"Cannot revoke / renew access for accessor who didn't gain access via your submission.");
		}

		ValidateArgument.requirement(accessorsWillHaveAccess.contains(userInfo.getId().toString()),
				"Submitter has to be an accessor.");
		submissionToCreate.setAccessorChanges(request.getAccessorChanges());

		if (request instanceof Renewal) {
			submissionToCreate.setIsRenewalSubmission(true);
			Renewal renewalRequest = (Renewal) request;
			submissionToCreate.setPublication(renewalRequest.getPublication());
			submissionToCreate.setSummaryOfUse(renewalRequest.getSummaryOfUse());
		} else {
			submissionToCreate.setIsRenewalSubmission(false);
		}
	}

	/**
	 * @param userInfo
	 * @param submissionToCreate
	 */
	public void prepareCreationFields(UserInfo userInfo, Submission submissionToCreate) {
		submissionToCreate.setSubmittedBy(userInfo.getId().toString());
		submissionToCreate.setSubmittedOn(new Date());
		submissionToCreate.setModifiedBy(userInfo.getId().toString());
		submissionToCreate.setModifiedOn(new Date());
		submissionToCreate.setState(SubmissionState.SUBMITTED);
	}

	@WriteTransaction
	@Override
	public SubmissionStatus cancel(UserInfo userInfo, String submissionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(submissionId, "submissionId");
		Submission submission = submissionDao.getForUpdate(submissionId);
		if (!submission.getSubmittedBy().equals(userInfo.getId().toString())) {
			throw new UnauthorizedException("Can only cancel submission you submitted.");
		}
		ValidateArgument.requirement(submission.getState().equals(SubmissionState.SUBMITTED),
						"Cannot cancel a submission with "+submission.getState()+" state.");
		return submissionDao.cancel(submissionId, userInfo.getId().toString(),
				System.currentTimeMillis(), UUID.randomUUID().toString());
	}

	@WriteTransaction
	@Override
	public Submission updateStatus(UserInfo userInfo, SubmissionStateChangeRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSubmissionId(), "submissionId");
		ValidateArgument.required(request.getNewState(), "newState");
		ValidateArgument.requirement(request.getNewState().equals(SubmissionState.APPROVED)
				|| request.getNewState().equals(SubmissionState.REJECTED),
				"Do not support changing to state: "+request.getNewState());
				
		Submission submission = submissionDao.getForUpdate(request.getSubmissionId());
		
		authorizationManager.canReviewAccessRequirementSubmissions(userInfo, submission.getAccessRequirementId()).checkAuthorizationOrElseThrow();
		
		ValidateArgument.requirement(submission.getState().equals(SubmissionState.SUBMITTED),
						"Cannot change state of a submission with "+submission.getState()+" state.");
		
		if (request.getNewState().equals(SubmissionState.APPROVED)) {
			ManagedACTAccessRequirement ar = (ManagedACTAccessRequirement)accessRequirementDao.get(submission.getAccessRequirementId());
			Date expiredOn = calculateExpiredOn(ar.getExpirationPeriod());
			
			List<AccessApproval> approvalsToCreateOrUpdate = new ArrayList<AccessApproval>();
			List<String> accessorsToRevoke = new LinkedList<String>();
			
			String modifiedBy =  userInfo.getId().toString();
			
			createApprovalsForSubmission(submission, modifiedBy, expiredOn, approvalsToCreateOrUpdate, accessorsToRevoke);

			if (!accessorsToRevoke.isEmpty()) {
				accessAprovalManager.revokeGroup(userInfo, submission.getAccessRequirementId(), submission.getSubmittedBy(), accessorsToRevoke);
			}
			
			if (!approvalsToCreateOrUpdate.isEmpty()) {
				accessApprovalDao.createOrUpdateBatch(approvalsToCreateOrUpdate);
			}
			/*
			 * See PLFM-4442.
			 */
			requestManager.updateApprovedRequest(submission.getRequestId());
		}
		
		submission = submissionDao.updateSubmissionStatus(request.getSubmissionId(),
				request.getNewState(), request.getRejectedReason(), userInfo.getId().toString(),
				System.currentTimeMillis());
		
		MessageToSend changeMessage = new MessageToSend()
				.withUserId(userInfo.getId())
				.withObjectType(ObjectType.DATA_ACCESS_SUBMISSION_STATUS)
				.withObjectId(submission.getId())
				.withChangeType(ChangeType.UPDATE);
		
		transactionalMessenger.sendMessageAfterCommit(changeMessage);
		
		return submission;
	}

	public static Date calculateExpiredOn(Long expirationPeriod) {
		ValidateArgument.required(expirationPeriod, "expirationPeriod");
		if (expirationPeriod.equals(AccessRequirementManagerImpl.DEFAULT_EXPIRATION_PERIOD)) {
			return null;
		}
		return new Date(System.currentTimeMillis() + expirationPeriod);
	}

	public void createApprovalsForSubmission(Submission submission, String createdBy,
			Date expiredOn, List<AccessApproval> approvalsToCreateOrUpdate,
			List<String> accessorsToRevoke) {
		Date createdOn = new Date();
		Long requirementId = Long.parseLong(submission.getAccessRequirementId());
		for (AccessorChange ac : submission.getAccessorChanges()) {
			switch (ac.getType()) {
				case REVOKE_ACCESS:
					accessorsToRevoke.add(ac.getUserId());
					break;
				case RENEW_ACCESS:
					accessorsToRevoke.add(ac.getUserId());
				case GAIN_ACCESS:
					AccessApproval approval = new AccessApproval();
					approval.setAccessorId(ac.getUserId());
					approval.setCreatedBy(createdBy);
					approval.setCreatedOn(createdOn);
					approval.setModifiedBy(createdBy);
					approval.setModifiedOn(createdOn);
					approval.setRequirementId(requirementId);
					approval.setRequirementVersion(submission.getAccessRequirementVersion());
					approval.setSubmitterId(submission.getSubmittedBy());
					approval.setExpiredOn(expiredOn);
					approval.setState(ApprovalState.APPROVED);
					approvalsToCreateOrUpdate.add(approval);
					break;
			}
		}
	}

	@Override
	public SubmissionPage listSubmission(UserInfo userInfo, SubmissionPageRequest request){
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAccessRequirementId(), "accessRequirementId");
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken token = new NextPageToken(request.getNextPageToken());
		List<Submission> submissions = submissionDao.getSubmissions(
				request.getAccessRequirementId(), request.getFilterBy(), request.getAccessorId(), request.getOrderBy(),
				request.getIsAscending(), token.getLimitForQuery(), token.getOffset());
		SubmissionPage pageResult = new SubmissionPage();
		pageResult.setResults(submissions);
		pageResult.setNextPageToken(token.getNextPageTokenForCurrentResults(submissions));
		return pageResult;
	}

	@WriteTransaction
	@Override
	public void deleteSubmission(UserInfo userInfo, String submissionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(submissionId, "submissionId");

		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		submissionDao.delete(submissionId);
	}

	@Override
	public SubmissionInfoPage listInfoForApprovedSubmissions(UserInfo userInfo, SubmissionInfoPageRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAccessRequirementId(), "accessRequirementId");
		AccessRequirement ar = accessRequirementDao.get(request.getAccessRequirementId());
		if (!(ar instanceof ManagedACTAccessRequirement)) {
			throw new IllegalArgumentException("Cannot list research projects for an access requirement which is not a Managed ACT Access Requirement");
		}
		if (!((ManagedACTAccessRequirement)ar).getIsIDUPublic()) {
			throw new IllegalArgumentException("Cannot list research projects for an access requirement whose IDUs are not public.");
		}
		NextPageToken token = new NextPageToken(request.getNextPageToken());
		boolean isACTorAdmin = AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo);
		List<SubmissionInfo> submissionInfoList = submissionDao.listInfoForApprovedSubmissions(
				request.getAccessRequirementId(), token.getLimitForQuery(), token.getOffset(), isACTorAdmin);
		
		SubmissionInfoPage pageResult = new SubmissionInfoPage();
		pageResult.setResults(submissionInfoList);
		pageResult.setNextPageToken(token.getNextPageTokenForCurrentResults(submissionInfoList));
		return pageResult;
	}

	@Override
	public AccessRequirementStatus getAccessRequirementStatus(UserInfo userInfo, String accessRequirementId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		String concreteType = accessRequirementDao.getConcreteType(accessRequirementId);
		List<AccessApproval> approvals = accessApprovalDao.getActiveApprovalsForUser(
				accessRequirementId, userInfo.getId().toString());

		boolean isApproved = !approvals.isEmpty();
		Date expiredOn = null;
		if (isApproved) {
			expiredOn = getLatestExpirationDate(approvals);
		}

		if (concreteType.equals(ManagedACTAccessRequirement.class.getName())) {
			ManagedACTAccessRequirementStatus status = new ManagedACTAccessRequirementStatus();
			SubmissionStatus currentSubmissionStatus = submissionDao.getStatusByRequirementIdAndPrincipalId(
					accessRequirementId, userInfo.getId().toString());
			setApprovalStatus(accessRequirementId, isApproved, expiredOn, status);
			status.setCurrentSubmissionStatus(currentSubmissionStatus);
			return status;
		} else {
			BasicAccessRequirementStatus status = new BasicAccessRequirementStatus();
			setApprovalStatus(accessRequirementId, isApproved, expiredOn, status);
			return status;
		}
	}
	
	@Override
	public Submission getSubmission(UserInfo userInfo, String submissionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(submissionId, "submissionId");
		
		Submission submission = submissionDao.getSubmission(submissionId);
		
		authorizationManager.canReviewAccessRequirementSubmissions(userInfo, submission.getAccessRequirementId())
			.checkAuthorizationOrElseThrow();

		return submission;
	}
	
	@Override
	public SubmissionSearchResponse searchSubmissions(UserInfo userInfo, SubmissionSearchRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		
		NextPageToken pageToken = new NextPageToken(request.getNextPageToken());
		
		long limit = pageToken.getLimitForQuery();
		long offset = pageToken.getOffset();
		
		boolean isACTMember = AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo);
		String accessorId = request.getAccessorId();
		String requirementId = request.getAccessRequirementId();
		List<SubmissionSearchSort> sort = request.getSort() == null || request.getSort().isEmpty() ? List.of(new SubmissionSearchSort().setField(SubmissionSortField.CREATED_ON)) : request.getSort();
		SubmissionState state = request.getSubmissionState();
		String reviewerId = request.getReviewerId();
		SubmissionReviewerFilterType reviewerFilterType = request.getReviewerFilterType() == null ? SubmissionReviewerFilterType.ALL : request.getReviewerFilterType();
		
		List<Submission> submissionPage;
		
		if (isACTMember) {
			submissionPage = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset);
		} else {
			// A non-ACT user cannot see ACT_ONLY submissions
			switch (reviewerFilterType) {
			// For a non-ACT user ALL=DELEGATED_ONLY
			case ALL:
			case DELEGATED_ONLY:
				submissionPage = submissionDao.searchSubmissionsReviewableByGroups(userInfo.getGroups(), sort, accessorId, requirementId, reviewerId, state, limit, offset);	
				break;
			default:
				submissionPage = Collections.emptyList();
				break;
			}
		}
		
		if (submissionPage.isEmpty()) {
			return new SubmissionSearchResponse()
				.setResults(Collections.emptyList());
		}
		
		String nextPageToken = pageToken.getNextPageTokenForCurrentResults(submissionPage);
		
		Set<Long> arIdsSet = submissionPage.stream().map(s -> Long.valueOf(s.getAccessRequirementId())).collect(Collectors.toSet());
		Map<Long, String> arNamesMap = accessRequirementDao.getAccessRequirementNames(arIdsSet);
		Map<Long, List<String>> arReviewersMap = authorizationManager.getAccessRequirementReviewers(arIdsSet);
		
		List<SubmissionSearchResult> result = submissionPage.stream().map( submission -> {
			Long arId = Long.valueOf(submission.getAccessRequirementId());
			
			return new SubmissionSearchResult()
				.setId(submission.getId())
				.setAccessRequirementId(submission.getAccessRequirementId())
				.setAccessRequirementVersion(submission.getAccessRequirementVersion() == null ? null : submission.getAccessRequirementVersion().toString())
				.setAccessRequirementName(arNamesMap.get(arId))
				.setAccessRequirementReviewerIds(arReviewersMap.getOrDefault(arId, Collections.emptyList()))
				.setAccessorChanges(submission.getAccessorChanges())
				.setCreatedOn(submission.getSubmittedOn())
				.setModifiedOn(submission.getModifiedOn())
				.setState(submission.getState())
				.setSubmitterId(submission.getSubmittedBy());
		}).collect(Collectors.toList());
		
		return new SubmissionSearchResponse()
			.setResults(result)
			.setNextPageToken(nextPageToken);
	}

	/**
	 * @param accessRequirementId
	 * @param isApproved
	 * @param expiredOn
	 * @param status
	 */
	public void setApprovalStatus(String accessRequirementId, boolean isApproved, Date expiredOn,
			AccessRequirementStatus status) {
		status.setAccessRequirementId(accessRequirementId);
		status.setIsApproved(isApproved);
		status.setExpiredOn(expiredOn);
	}

	/**
	 * @param approvals
	 * @param expiredOn
	 * @return
	 */
	public static Date getLatestExpirationDate(List<AccessApproval> approvals) {
		ValidateArgument.required(approvals, "approvals");
		Date expiredOn = null;
		for (AccessApproval approval : approvals) {
			if (approval.getExpiredOn() != null
					&& (expiredOn == null || approval.getExpiredOn().after(expiredOn))) {
					expiredOn = approval.getExpiredOn();
			}
		}
		return expiredOn;
	}

	@Override
	public OpenSubmissionPage getOpenSubmissions(UserInfo userInfo, String nextPageToken) {
		ValidateArgument.required(userInfo, "userInfo");
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken token = new NextPageToken(nextPageToken);
		OpenSubmissionPage result = new OpenSubmissionPage();
		List<OpenSubmission> openSubmissionList = submissionDao.getOpenSubmissions(token.getLimitForQuery(), token.getOffset());
		result.setOpenSubmissionList(openSubmissionList);
		result.setNextPageToken(token.getNextPageTokenForCurrentResults(openSubmissionList));
		return result;
	}

	public void truncateAll() {
		submissionDao.truncateAll();
	}
}

package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class AccessRequirementUtil {
	
	private static final List<Long> EMPTY_LIST = Arrays.asList(new Long[]{});
	public static List<Long> unmetAccessRequirementIds(
			UserInfo userInfo, 
			RestrictableObjectDescriptor subjectId,
			NodeDAO nodeDAO, 
			NodeTreeQueryDao nodeTreeQueryDao,
			AccessRequirementDAO accessRequirementDAO
			) throws NotFoundException {
		List<ACCESS_TYPE> accessTypes = new ArrayList<ACCESS_TYPE>();
		List<String> subjectIds = new ArrayList<String>();
		subjectIds.add(subjectId.getId());
		if (RestrictableObjectType.ENTITY.equals(subjectId.getType())) {
			// if the user is the owner of the object, then she automatically 
			// has access to the object and therefore has no unmet access requirements
			Long principalId = userInfo.getId();
			Node node = nodeDAO.getNode(subjectId.getId());
			if (node.getCreatedByPrincipalId().equals(principalId)) {
				return EMPTY_LIST;
			}
			accessTypes.add(ACCESS_TYPE.DOWNLOAD);
			// per PLFM-2477, we inherit the restrictions of the node's ancestors
			subjectIds.addAll(nodeTreeQueryDao.getAncestors(subjectId.getId()));
		} else if (RestrictableObjectType.EVALUATION.equals(subjectId.getType())) {
			accessTypes.add(ACCESS_TYPE.DOWNLOAD);
			accessTypes.add(ACCESS_TYPE.PARTICIPATE);
		} else if (RestrictableObjectType.TEAM.equals(subjectId.getType())) {
			accessTypes.add(ACCESS_TYPE.DOWNLOAD);
			accessTypes.add(ACCESS_TYPE.PARTICIPATE);
		} else {
			throw new IllegalArgumentException("Unexpected type: "+subjectId.getType());
		}

		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}
		
		return accessRequirementDAO.unmetAccessRequirements(subjectIds, subjectId.getType(), principalIds, accessTypes);
	}
}

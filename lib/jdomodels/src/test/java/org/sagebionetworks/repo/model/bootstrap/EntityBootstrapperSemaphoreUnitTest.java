package org.sagebionetworks.repo.model.bootstrap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
public class EntityBootstrapperSemaphoreUnitTest {
	
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private UserGroupDAO mockUserGroupDao;
	@Mock
	private UserProfileDAO mockUserProfileDao;
	@Mock
	private GroupMembersDAO mockGroupMembersDao;
	@Mock
	private AuthenticationDAO mockAuthenticationDao;
	@Mock
	private AccessControlListDAO mockAclDao;
	@Mock
	private CountingSemaphore mockSemaphoreDao;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	
	private List<EntityBootstrapData> bootstrapData;
	
	@InjectMocks
	EntityBootstrapperImpl bootstrapper;
	
	Node node;

	@BeforeEach
	public void before() throws Exception {
		// Setup EntityBootstrapData
		bootstrapData = new ArrayList<EntityBootstrapData>();
		EntityBootstrapData ebd = new EntityBootstrapData();
		ebd.setEntityId(1001L);
		ebd.setEntityType(EntityType.folder);
		ebd.setEntityPath("/root");
		ebd.setDefaultChildAclScheme(ACL_SCHEME.INHERIT_FROM_PARENT);
		List<AccessBootstrapData> accessList = new ArrayList<AccessBootstrapData>();
		ebd.setAccessList(accessList);
		bootstrapData.add(ebd);
		ReflectionTestUtils.setField(bootstrapper, "bootstrapEntities", bootstrapData);
		
		node = new Node();
		node.setId("syn123");
	}


	@Test
	public void testBootsrapSemaphore() throws Exception {
		when(mockSemaphoreDao.attemptToAcquireLock("ENTITYBOOTSTRAPPERLOCK", 30L, 1)).thenReturn(null, null, null, "token");
		when(mockNodeDao.getNodeIdForPath(bootstrapData.get(0).getEntityPath())).thenReturn(null, bootstrapData.get(0).getEntityId().toString()); // Should force node creation
		when(mockNodeDao.bootstrapNode(any(Node.class), any(Long.class))).thenReturn(node);
		bootstrapper.bootstrapAll();
		verify(mockSemaphoreDao, times(4)).attemptToAcquireLock("ENTITYBOOTSTRAPPERLOCK", 30L, 1);
		verify(mockUserGroupDao).bootstrapUsers();
		verify(mockUserProfileDao).bootstrapProfiles();
		verify(mockGroupMembersDao).bootstrapGroups();
		verify(mockAuthenticationDao).bootstrapCredentials();
		verify(mockAccessRequirementDao).bootstrap();
		verify(mockNodeDao).bootstrapNode(any(Node.class), any(Long.class));
		verify(mockAclDao).create(any(AccessControlList.class), any(ObjectType.class));
		verify(mockSemaphoreDao).releaseLock("ENTITYBOOTSTRAPPERLOCK", "token");
	}
	
	@Test
	public void testBootstrapSemaphoreException() throws Exception {
		when(mockSemaphoreDao.attemptToAcquireLock("ENTITYBOOTSTRAPPERLOCK", 30L, 1)).thenReturn(null, null, null, "token");
		when(mockNodeDao.getNodeIdForPath(bootstrapData.get(0).getEntityPath())).thenReturn(null, bootstrapData.get(0).getEntityId().toString()); // Should force node creation
		when(mockNodeDao.bootstrapNode(any(Node.class), any(Long.class))).thenThrow(new IllegalArgumentException());
		assertThrows(IllegalArgumentException.class, ()->{
			bootstrapper.bootstrapAll();
		});
		verify(mockSemaphoreDao, times(4)).attemptToAcquireLock("ENTITYBOOTSTRAPPERLOCK", 30L, 1);
		verify(mockUserGroupDao).bootstrapUsers();
		verify(mockUserProfileDao).bootstrapProfiles();
		verify(mockGroupMembersDao).bootstrapGroups();
		verify(mockAuthenticationDao).bootstrapCredentials();
		verify(mockSemaphoreDao).releaseLock("ENTITYBOOTSTRAPPERLOCK", "token");
	}

}

package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class EntityObjectProviderTest {
	
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private ObjectDataDTO mockData;
	
	@InjectMocks
	private EntityObjectProvider provider;
	
	@Mock
	private IdAndEtag mockIdAndEtag;

	
	@Test
	public void testGetObjectData() {

		List<ObjectDataDTO> expected = Collections.singletonList(mockData);
		List<ObjectDataDTO> empty = Collections.emptyList();

		List<Long> objectIds = ImmutableList.of(1L, 2L, 3L);

		int maxAnnotationChars = 5;

		when(mockNodeDao.getEntityDTOs(any(), anyInt(), anyLong(), anyLong())).thenReturn(expected, empty);

		// Call under test
		Iterator<ObjectDataDTO> iterator = provider.getObjectData(objectIds, maxAnnotationChars);
		List<ObjectDataDTO> result = new ArrayList<ObjectDataDTO>();
		iterator.forEachRemaining(result::add);

		assertEquals(expected, result);
		verify(mockNodeDao).getEntityDTOs(objectIds, maxAnnotationChars, EntityObjectProvider.PAGE_SIZE, 0L);
		verify(mockNodeDao).getEntityDTOs(objectIds, maxAnnotationChars, EntityObjectProvider.PAGE_SIZE, EntityObjectProvider.PAGE_SIZE);
	}
	
	@Test
	public void testGetAvaliableContainers() {

		List<Long> containerIds = ImmutableList.of(1L, 2L);
		Set<Long> expectedIds = ImmutableSet.of(1L);

		when(mockNodeDao.getAvailableNodes(any())).thenReturn(expectedIds);

		// Call under test
		Set<Long> result = provider.getAvailableContainers(containerIds);

		assertEquals(expectedIds, result);
		verify(mockNodeDao).getAvailableNodes(containerIds);
	}

	@Test
	public void testGetChildren() {

		Long containerId = 1L;
		List<IdAndEtag> expected = ImmutableList.of(mockIdAndEtag, mockIdAndEtag);

		when(mockNodeDao.getChildren(anyLong())).thenReturn(expected);

		// Call under test
		List<IdAndEtag> result = provider.getChildren(containerId);

		assertEquals(expected, result);
		verify(mockNodeDao).getChildren(containerId);
	}

	@Test
	public void testGetSumOfChildCRCsForEachContainer() {
		List<Long> containerIds = ImmutableList.of(1L, 2L);

		Map<Long, Long> expected = ImmutableMap.of(1L, 10L, 2L, 30L);

		when(mockNodeDao.getSumOfChildCRCsForEachParent(any())).thenReturn(expected);

		Map<Long, Long> result = provider.getSumOfChildCRCsForEachContainer(containerIds);

		assertEquals(expected, result);
		verify(mockNodeDao).getSumOfChildCRCsForEachParent(containerIds);
	}
}

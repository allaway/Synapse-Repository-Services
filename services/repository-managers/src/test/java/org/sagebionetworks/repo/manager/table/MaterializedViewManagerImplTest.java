package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.QuerySpecification;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewManagerImplTest {

	@Mock
	private MaterializedViewDao mockDao;

	@Mock
	private ColumnModelManager mockColumnModelManager;
	
	@Mock
	private TableManagerSupport mockTableManagerSupport;
	
	@Mock
	private ProgressCallback mockProgressCallback;
	
	@Mock
	private TableIndexConnectionFactory mockConnectionFactory;
	
	@Mock
	private TableIndexManager mockTableIndexManager;
	
	@Mock
	private NodeDAO mockNodeDao;

	@InjectMocks
	private MaterializedViewManagerImpl manager;
	
	private MaterializedViewManagerImpl managerSpy;

	@Mock
	private MaterializedView mockView;

	private IdAndVersion idAndVersion = IdAndVersion.parse("syn123.1");

	private List<ColumnModel> syn123Schema;

	@BeforeEach
	public void before() {
		syn123Schema = Arrays.asList(TableModelTestUtils.createColumn(111L, "foo", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(222L, "bar", ColumnType.STRING));
		
		managerSpy = Mockito.spy(manager);
	}

	@Test
	public void testValidate() {
		String sql = "SELECT * FROM syn123";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		// Call under test
		manager.validate(mockView);

		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithNullSQL() {
		String sql = null;

		when(mockView.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithEmptySQL() {
		String sql = "";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithBlankSQL() {
		String sql = "   ";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be a blank string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithInvalidSQL() {
		String sql = "invalid SQL";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		});

		assertTrue(ex.getCause() instanceof ParseException);

		assertTrue(ex.getMessage().startsWith("Encountered \" <regular_identifier> \"invalid"));
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithWithNoTable() {
		String sql = "SELECT foo";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		});

		assertTrue(ex.getCause() instanceof ParseException);

		assertTrue(ex.getMessage().startsWith("Encountered \"<EOF>\" at line 1, column 10."));
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithWithUnsupportedJoin() {
		String sql = "SELECT * FROM table1 JOIN table2";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		}).getMessage();

		assertEquals("The JOIN keyword is not supported in this context", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testRegisterSourceTables() {

		Set<IdAndVersion> currentSourceTables = Collections.emptySet();
		String sql = "SELECT * FROM syn123";

		Set<IdAndVersion> expectedDeletes = Collections.emptySet();
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"));

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verify(mockDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithNonOverlappingAssociations() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn456"));

		String sql = "SELECT * FROM syn123";

		Set<IdAndVersion> expectedDeletes = currentSourceTables;
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"));

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verify(mockDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
		
	}

	@Test
	public void testRegisterSourceTablesWithOverlappingAssociations() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"),
				IdAndVersion.parse("syn456"));

		String sql = "SELECT * FROM syn123";

		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(IdAndVersion.parse("syn456"));
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"));

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verify(mockDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithNoChanges() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"));

		String sql = "SELECT * FROM syn123";

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verifyNoMoreInteractions(mockDao);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithMultipleTables() {

		Set<IdAndVersion> currentSourceTables = Collections.emptySet();
		String sql = "SELECT * FROM syn123 JOIN syn456";

		Set<IdAndVersion> expectedDeletes = Collections.emptySet();
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456"));

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verify(mockDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithMultipleTablesWithNonOverlappingAssociations() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn789"),
				IdAndVersion.parse("syn101112"));
		String sql = "SELECT * FROM syn123 JOIN syn456";

		Set<IdAndVersion> expectedDeletes = currentSourceTables;
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456"));

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verify(mockDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
		
	}

	@Test
	public void testRegisterSourceTablesWithMultipleTablesWithOverlappingAssociations() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn789"), IdAndVersion.parse("syn101112"));
		String sql = "SELECT * FROM syn123 JOIN syn456";

		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(IdAndVersion.parse("syn789"),
				IdAndVersion.parse("syn101112"));
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456"));

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verify(mockDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithMultipleTablesAndNoChanges() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn123"));

		String sql = "SELECT * FROM syn123 JOIN syn456";

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verifyNoMoreInteractions(mockDao);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithVersions() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn123.2"));

		String sql = "SELECT * FROM syn123.3 JOIN syn456";

		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(IdAndVersion.parse("syn123.2"));

		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123.3"),
				IdAndVersion.parse("syn456"));

		when(mockDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any());

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockDao).getSourceTablesIds(idAndVersion);
		verify(mockDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any());
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithNoIdAndVersion() {

		idAndVersion = null;
		String sql = "SELECT * FROM syn123";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.registerSourceTables(idAndVersion, sql);
		}).getMessage();

		assertEquals("The id of the materialized view is required.", message);

		verifyZeroInteractions(mockDao);

	}

	@Test
	public void testRegisterSourceTablesWithNoSql() {

		String sql = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.registerSourceTables(idAndVersion, sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);

		verifyZeroInteractions(mockDao);

	}

	@Test
	public void testRegisterSourceTablesWithEmptySql() {

		String sql = "";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.registerSourceTables(idAndVersion, sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);

		verifyZeroInteractions(mockDao);

	}

	@Test
	public void testRegisterSourceTablesWithBlankSql() {

		String sql = "   ";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.registerSourceTables(idAndVersion, sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be a blank string.", message);

		verifyZeroInteractions(mockDao);

	}

	@Test
	public void testGetQuerySpecification() {
		String sql = "SELECT * FROM syn123";

		QuerySpecification result = MaterializedViewManagerImpl.getQuerySpecification(sql);

		assertNotNull(result);

	}

	@Test
	public void testGetQuerySpecificationWithParingException() {
		String sql = "invalid query";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			MaterializedViewManagerImpl.getQuerySpecification(sql);
		}).getMessage();

		assertTrue(message.startsWith("Encountered \" <regular_identifier>"));
	}

	@Test
	public void testGetQuerySpecificationWithNullQuery() {
		String sql = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			MaterializedViewManagerImpl.getQuerySpecification(sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
	}

	@Test
	public void testGetQuerySpecificationWithEmptyQuery() {
		String sql = "";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			MaterializedViewManagerImpl.getQuerySpecification(sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
	}

	@Test
	public void testGetQuerySpecificationWithBlankQuery() {
		String sql = "   ";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			MaterializedViewManagerImpl.getQuerySpecification(sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be a blank string.", message);
	}

	@Test
	public void testGetSourceTableIds() {

		QuerySpecification query = MaterializedViewManagerImpl.getQuerySpecification("SELECT * FROM syn123");

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"));
		Set<IdAndVersion> result = MaterializedViewManagerImpl.getSourceTableIds(query);

		assertEquals(expected, result);
	}

	@Test
	public void testGetSourceTableIdsWithVersion() {

		QuerySpecification query = MaterializedViewManagerImpl.getQuerySpecification("SELECT * FROM syn123.1");

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123.1"));
		Set<IdAndVersion> result = MaterializedViewManagerImpl.getSourceTableIds(query);

		assertEquals(expected, result);
	}

	@Test
	public void testGetSourceTableIdsWithMultiple() {

		QuerySpecification query = MaterializedViewManagerImpl
				.getQuerySpecification("SELECT * FROM syn123.1 JOIN syn456 JOIN syn123");

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn123.1"),
				IdAndVersion.parse("456"));
		Set<IdAndVersion> result = MaterializedViewManagerImpl.getSourceTableIds(query);

		assertEquals(expected, result);
	}

	@Test
	public void testBindSchemaToView() {
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn123Schema);
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(
				TableModelTestUtils.createColumn(333L, "foo", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(444L, "bar", ColumnType.STRING));
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		QuerySpecification query = MaterializedViewManagerImpl.getQuerySpecification("SELECT * FROM syn123");
		// call under test
		manager.bindSchemaToView(idAndVersion, query);
		verify(mockColumnModelManager).getTableSchema(idAndVersion);
		verify(mockColumnModelManager)
				.createColumnModel(new ColumnModel().setName("foo").setColumnType(ColumnType.INTEGER).setId(null));
		verify(mockColumnModelManager).createColumnModel(
				new ColumnModel().setName("bar").setColumnType(ColumnType.STRING).setMaximumSize(50L).setId(null));
		verify(mockColumnModelManager).bindColumnsToVersionOfObject(Arrays.asList("333", "444"), idAndVersion);
	}
	
	@Test
	public void testDeleteViewIndex() {
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		
		// call under test
		manager.deleteViewIndex(idAndVersion);
		
		verify(mockConnectionFactory).connectToTableIndex(idAndVersion);
		verify(mockTableIndexManager).deleteTableIndex(idAndVersion);
	}
	
	@Test
	public void testCreateOrUpdateViewIndex() throws Exception {
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[2];
			runner.call(callback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(), any(IdAndVersion.class), any());
		doNothing().when(managerSpy).createOrRebuildViewHoldingExclusiveLock(any(), any());
		// call under test
		managerSpy.createOrUpdateViewIndex(mockProgressCallback, idAndVersion);
		
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(idAndVersion), any());
		verify(managerSpy).createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, idAndVersion);
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingExclusiveLock() throws Exception {
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[1];
			runner.call(callback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableNonexclusiveLock(any(), any(), any());
		
		when(mockNodeDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.of("select * from syn456"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn123Schema);
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(
				TableModelTestUtils.createColumn(333L, "foo", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(444L, "bar", ColumnType.STRING));
		IdAndVersion dependentIdAndVersion = IdAndVersion.parse("syn456");
		
		// call under test
		managerSpy.createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, idAndVersion);
		
		verify(mockTableManagerSupport).tryRunWithTableNonexclusiveLock(eq(mockProgressCallback), any(), eq( dependentIdAndVersion));
		verify(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(idAndVersion);
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingExclusiveLockWithNoDefiningSql() throws Exception {
		
		when(mockNodeDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.empty());
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, idAndVersion);
		}).getMessage();
		assertEquals("No defining SQL for: syn123.1", message);
		
		verify(mockTableManagerSupport, never()).tryRunWithTableNonexclusiveLock(any(), any(), any());
		verify(managerSpy, never()).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(any());
	}

}

package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.MaterializedViewIndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SqlContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class QueryTranslatorTest {

	Map<String, ColumnModel> columnNameToModelMap;
	List<ColumnModel> tableSchema;

	private static final String DOUBLE_COLUMN = "CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END";
	private static final String STAR_COLUMNS = "_C111_, _C222_, _C333_, _C444_, _C555_, _C666_, " + DOUBLE_COLUMN
			+ ", _C888_, _C999_, _C4242_";

	private ColumnModel cm;
	private List<ColumnModel> schema;
	private String sql;
	private Long userId;
	private IdAndVersion idAndVersion;

	@BeforeEach
	public void before() {
		columnNameToModelMap = Maps.newLinkedHashMap(); // retains order of values
		columnNameToModelMap.put("foo", TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING));
		columnNameToModelMap.put("has space", TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING));
		columnNameToModelMap.put("bar", TableModelTestUtils.createColumn(333L, "bar", ColumnType.STRING));
		columnNameToModelMap.put("foo_bar", TableModelTestUtils.createColumn(444L, "foo_bar", ColumnType.STRING));
		columnNameToModelMap.put("Foo", TableModelTestUtils.createColumn(555L, "Foo", ColumnType.STRING));
		columnNameToModelMap.put("datetype", TableModelTestUtils.createColumn(666L, "datetype", ColumnType.DATE));
		columnNameToModelMap.put("doubletype", TableModelTestUtils.createColumn(777L, "doubletype", ColumnType.DOUBLE));
		columnNameToModelMap.put("inttype", TableModelTestUtils.createColumn(888L, "inttype", ColumnType.INTEGER));
		columnNameToModelMap.put("has-hyphen", TableModelTestUtils.createColumn(999L, "has-hyphen", ColumnType.STRING));
		columnNameToModelMap.put("has\"quote",
				TableModelTestUtils.createColumn(4242L, "has\"quote", ColumnType.STRING));
		tableSchema = new ArrayList<ColumnModel>(columnNameToModelMap.values());

		cm = new ColumnModel();
		cm.setName("5ormore");
		cm.setColumnType(ColumnType.INTEGER);
		cm.setId("111");
		schema = Lists.newArrayList(cm);
		sql = "select * from syn123";
		userId = 1l;
		idAndVersion = IdAndVersion.parse("syn123");
	}

	@Test
	public void testSelectStarEmtpySchema() throws ParseException {
		tableSchema = new LinkedList<ColumnModel>();

		assertThrows(IllegalArgumentException.class, () -> {
			QueryTranslator.builder("select * from syn123", schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		});
	}

	@Test
	public void testSelectStar() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123", schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		assertNotNull(translator.getSelectColumns());
		assertEquals(translator.getSelectColumns().size(), 10);
		assertEquals(TableModelUtils.getSelectColumns(this.tableSchema), translator.getSelectColumns());
	}

	@Test
	public void testSelectStarEscaping() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123", schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		translator = QueryTranslator.builder(sql, schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testSelectSingleColumns() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"));
		assertEquals(TableModelUtils.getSelectColumns(expectedSelect), translator.getSelectColumns());
	}
	
	@Test
	public void testSqlQueryBuildWithNullSqlContext() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).sqlContext(null).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertEquals(SqlContext.query, translator.getSqlContext());
	}
	
	@Test
	public void testSqlQueryBuildWithSqlContextQuery() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).sqlContext(SqlContext.query).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertEquals(SqlContext.query, translator.getSqlContext());
	}
	
	@Test
	public void testSqlQueryBuildWithSqlContextBuild() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList()))
				.sqlContext(SqlContext.build).build();
		assertEquals("SELECT _C111_ FROM T123", translator.getOutputSQL());
		assertEquals(SqlContext.build, translator.getSqlContext());
	}

	@Test
	public void testSelectDoubleQuotedColumn() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select \"has\"\"quote\" from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C4242_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("has\"quote"));
		assertEquals(TableModelUtils.getSelectColumns(expectedSelect), translator.getSelectColumns());
	}

	@Test
	public void testSelectMultipleColumns() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo, bar from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"),
				columnNameToModelMap.get("bar"));
		assertEquals(TableModelUtils.getSelectColumns(expectedSelect), translator.getSelectColumns());
	}

	@Test
	public void testSelectDistinct() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select distinct foo, bar from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT DISTINCT _C111_, _C333_ FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		List<SelectColumn> expectedSelect = Lists.newArrayList(
				TableModelUtils.createSelectColumn("foo", ColumnType.STRING, null),
				TableModelUtils.createSelectColumn("bar", ColumnType.STRING, null));
		assertEquals(expectedSelect, translator.getSelectColumns());
	}

	@Test
	public void selectRowIdAndVersionStar() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123", schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionSingleColumn() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionDistinct() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select distinct foo from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionCount() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select count(*) from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionAggregateFunction() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select max(foo) from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionNonAggreageFunction() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select concat('a',foo) from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionConstant() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select 'a constant' from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionConstantPlusColumn() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo, 'a constant' from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionArithmeticNoColumns() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select 5 div 2 from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionArithmeticAndColumn() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select 5 div 2, foo from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionArithmeticOfColumns() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select 5 div foo from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionGroupBy() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo, count(*) from syn123 group by foo",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionAggregateFunctionNoGroup() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo, max(bar) from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndNonAggregateFunction() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo, DAYOFMONTH(bar) from syn123",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndNonAggregateFunctionColumnRef() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select DAYOFMONTH(bar) from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndNonAggregateFunctionOnly() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select DAYOFMONTH('2017-12-12') from syn123",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndAs() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo as \"cats\" from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void testSelectConstant() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select 'not a foo' from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT 'not a foo', ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("not a foo", ColumnType.STRING, null)),
				translator.getSelectColumns());
		assertEquals("not a foo", translator.getSelectColumns().get(0).getName());
	}

	@Test
	public void testSelectCountStar() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select count(*) from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(*) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectAggregate() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select avg(inttype) from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT AVG(_C888_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectAggregateMoreColumns() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select avg(inttype), bar from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT AVG(_C888_), _C333_ FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(
				Lists.newArrayList(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null),
						TableModelUtils.createSelectColumn("bar", ColumnType.STRING, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectGroupByAggregate() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 group by foo", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_ FROM T123 GROUP BY _C111_", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("foo", ColumnType.STRING, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectAggregateMultiple() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select min(foo), max(bar) from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT MIN(_C111_), MAX(_C333_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(
				Lists.newArrayList(TableModelUtils.createSelectColumn("MIN(foo)", ColumnType.STRING, null),
						TableModelUtils.createSelectColumn("MAX(bar)", ColumnType.STRING, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectDistinctAggregate() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select count(distinct foo) from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(DISTINCT _C111_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(
				Lists.newArrayList(TableModelUtils.createSelectColumn("COUNT(DISTINCT foo)", ColumnType.INTEGER, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testWhereSimple() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where foo = 1", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
	}

	@Test
	public void testWhereDoubleFunction() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder(
				"select * from syn123 where isNaN(doubletype) or isInfinity(doubletype)", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE"
						+ " ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' ) "
						+ "OR ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )",
				translator.getOutputSQL());
	}

	@Test
	public void testWhereDouble() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 where doubletype between 1.0 and 2.0",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE _C777_ BETWEEN :b0 AND :b1",
				translator.getOutputSQL());
		assertEquals(new Double(1.0), translator.getParameters().get("b0"));
		assertEquals(new Double(2.0), translator.getParameters().get("b1"));
	}

	@Test
	public void testWhereOr() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where foo = 1 or bar = 2",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0 OR _C333_ = :b1",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
	}

	@Test
	public void testWhereAnd() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where foo = 1 and bar = 2",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0 AND _C333_ = :b1",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
	}

	@Test
	public void testWhereNested() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where (foo = 1 and bar = 2) or foo_bar = 3",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT " + STAR_COLUMNS
						+ ", ROW_ID, ROW_VERSION FROM T123 WHERE ( _C111_ = :b0 AND _C333_ = :b1 ) OR _C444_ = :b2",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
		assertEquals("3", translator.getParameters().get("b2"));
	}

	@Test
	public void testGroupByOne() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 group by foo", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + " FROM T123 GROUP BY _C111_", translator.getOutputSQL());
	}

	@Test
	public void testGroupByMultiple() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 group by foo, bar", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + " FROM T123 GROUP BY _C111_, _C333_", translator.getOutputSQL());
	}

	@Test
	public void testOrderByOneNoSpec() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 order by foo", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_",
				translator.getOutputSQL());
	}

	@Test
	public void testOrderByOneWithSpec() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 order by foo desc", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_ DESC",
				translator.getOutputSQL());
	}

	@Test
	public void testOrderByDouble() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 order by doubletype desc",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 ORDER BY _C777_ DESC", translator.getOutputSQL());
	}

	@Test
	public void testOrderByMultipleNoSpec() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 order by foo, bar", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_, _C333_",
				translator.getOutputSQL());
	}

	@Test
	public void testOrderByMultipeWithSpec() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 order by foo asc, bar desc",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_ ASC, _C333_ DESC",
				translator.getOutputSQL());
	}

	@Test
	public void testLimit() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 limit 100", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 LIMIT :b0", translator.getOutputSQL());
		assertEquals(100L, translator.getParameters().get("b0"));
	}

	@Test
	public void testLimitAndOffset() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 limit 100 offset 2",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1",
				translator.getOutputSQL());
		assertEquals(100L, translator.getParameters().get("b0"));
		assertEquals(2L, translator.getParameters().get("b1"));
	}

	@Test
	public void testAllParts() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder(
				"select foo, bar from syn123 where foo_bar >= 1.89e4 order by bar desc limit 10 offset 0",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123 WHERE _C444_ >= :b0 ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("18900.0", translator.getParameters().get("b0"));
		assertEquals(10L, translator.getParameters().get("b1"));
		assertEquals(0L, translator.getParameters().get("b2"));
	}

	@Test
	public void testAllPartsWithGrouping() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder(
				"select foo, bar from syn123 where foo_bar >= 1.89e4 group by foo order by bar desc limit 10 offset 0",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT _C111_, _C333_ FROM T123 WHERE _C444_ >= :b0 GROUP BY _C111_ ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("18900.0", translator.getParameters().get("b0"));
		assertEquals(10L, translator.getParameters().get("b1"));
		assertEquals(0L, translator.getParameters().get("b2"));
	}

	@Test
	public void testTypeSetFunctionStrings() throws Exception {
		QueryTranslator translator = QueryTranslator.builder("select count(*), min(foo), max(foo), count(foo) from syn123",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(*), MIN(_C111_), MAX(_C111_), COUNT(_C111_) FROM T123", translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("MIN(foo)", ColumnType.STRING, null),
				translator.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("MAX(foo)", ColumnType.STRING, null),
				translator.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(foo)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(3));
	}

	@Test
	public void testTypeSetFunctionIntegers() throws Exception {
		QueryTranslator translator = QueryTranslator.builder(
				"select min(inttype), max(inttype), sum(inttype), avg(inttype), count(inttype) from syn123",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT MIN(_C888_), MAX(_C888_), SUM(_C888_), AVG(_C888_), COUNT(_C888_) FROM T123",
				translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("MIN(inttype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("MAX(inttype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("SUM(inttype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(inttype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(4));
	}

	@Test
	public void testTypeSetFunctionDoubles() throws Exception {
		QueryTranslator translator = QueryTranslator.builder(
				"select min(doubletype), max(doubletype), sum(doubletype), avg(doubletype), count(doubletype) from syn123",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT MIN(_C777_), MAX(_C777_), SUM(_C777_), AVG(_C777_), COUNT(_C777_) FROM T123",
				translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("MIN(doubletype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("MAX(doubletype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("SUM(doubletype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("AVG(doubletype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(doubletype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(4));
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3864() throws Exception {
		QueryTranslator translator = QueryTranslator.builder("select doubletype as f1 from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT" + " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS f1"
				+ ", ROW_ID, ROW_VERSION" + " FROM T123", translator.getOutputSQL());
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3864withOrderBy() throws Exception {
		QueryTranslator translator = QueryTranslator.builder("select doubletype as f1 from syn123 order by f1",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT" + " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS f1"
				+ ", ROW_ID, ROW_VERSION" + " FROM T123 ORDER BY f1", translator.getOutputSQL());
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3865">3865</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3865() throws Exception {
		QueryTranslator translator = QueryTranslator.builder(
				"select max(doubletype), min(doubletype) from syn123 order by min(doubletype)",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT" + " MAX(_C777_)" + ", MIN(_C777_) " + "FROM T123 " + "ORDER BY MIN(_C777_)",
				translator.getOutputSQL());
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3865">3865</a>
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3865andPLFM_3864() throws Exception {
		QueryTranslator translator = QueryTranslator.builder(
				"select max(doubletype) as f1, min(doubletype) as f2 from syn123 order by f2",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT" + " MAX(_C777_) AS f1" + ", MIN(_C777_) AS f2 " + "FROM T123 " + "ORDER BY f2",
				translator.getOutputSQL());
	}

	/**
	 * We should be throwing 'column a not found' for this case.
	 * 
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3866">3866</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3866() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 where foo in (\"a\")",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ IN ( `a` )", translator.getOutputSQL());
	}

	@Test
	public void testPLFM_3867() throws ParseException {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			 QueryTranslator.builder("select foo from syn123 where foo = \"a\"",
					schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		}).getMessage();
		assertEquals("Column does not exist: a", message);
	}

	@Test
	public void testTranslateNotIsNaN() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 where not isNaN(doubletype)",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE NOT ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )",
				translator.getOutputSQL());
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3869">3869</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3869() throws ParseException {
		tableSchema = Lists.newArrayList(TableModelTestUtils.createColumn(123L, "aDouble", ColumnType.DOUBLE));
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where not isNaN(aDouble)",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT " + "CASE WHEN _DBL_C123_ IS NULL THEN _C123_ ELSE _DBL_C123_ END," + " ROW_ID, ROW_VERSION"
						+ " FROM T123 WHERE NOT ( _DBL_C123_ IS NOT NULL AND _DBL_C123_ = 'NaN' )",
				translator.getOutputSQL());
	}

	/**
	 * The casue of PLFM-3870 was double columns functions were translated as
	 * SUM(CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END) Therefore,
	 * the string 'NaN' was passed to the function and treated as '1'. To fix the
	 * issue we simply use the origial double column for aggregate function, for
	 * example SUM(_C777_)
	 * 
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3870">3870</a>
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testPLFM_3870() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select sum(doubletype) from syn123", schemaProvider(tableSchema),
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT SUM(_C777_) FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testTranslateIsNaN() throws ParseException {
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 where not isNaN(doubletype)",
				schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE NOT ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )",
				translator.getOutputSQL());
	}

	@Test
	public void testMaxRowSizeBytesSelectStar() throws ParseException {
		// the size will include the size of the schema
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		QueryTranslator translator = QueryTranslator.builder("select * from syn123", schemaProvider(tableSchema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
	}

	@Test
	public void testMaxRowSizeBytesCountStar() throws ParseException {
		// the size is the size of an integer
		int maxSizeSchema = TableModelUtils.calculateMaxSizeForType(ColumnType.INTEGER, null, null);
		QueryTranslator translator = QueryTranslator.builder("select count(*) from syn123", schemaProvider(tableSchema), userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
	}

	@Test
	public void testMaxRowsPerPage() throws ParseException {
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = maxSizeSchema * 2L;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, schemaProvider(tableSchema), maxBytesPerPage,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(new Long(2L), translator.getMaxRowsPerPage());
	}

	@Test
	public void testMaxRowsPerPageMaxBytesSmall() throws ParseException {
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = 3L;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, schemaProvider(tableSchema), maxBytesPerPage,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(new Long(1L), translator.getMaxRowsPerPage());
	}

	@Test
	public void testMaxRowsPerPageNullMaxBytesPerPage() throws ParseException {
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = null;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, schemaProvider(tableSchema), maxBytesPerPage,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(null, translator.getMaxRowsPerPage());
	}

	@Test
	public void testOverrideLimitAndOffset() throws ParseException {
		Long maxBytesPerPage = 10000L;
		QuerySpecification model = new TableQueryParser("select foo from syn123 limit 10 offset 1").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, schemaProvider(tableSchema),
				maxBytesPerPage, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1", translator.getOutputSQL());
		assertEquals(10L, translator.getParameters().get("b0"));
		assertEquals(1L, translator.getParameters().get("b1"));
	}

	@Test
	public void testOverrideLimitAndOffsetNullWithMaxBytesPerPage() throws ParseException {
		Long maxBytesPerPage = 1L;
		QuerySpecification model = new TableQueryParser("select foo from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, schemaProvider(tableSchema),
				maxBytesPerPage, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1", translator.getOutputSQL());
		assertEquals(1L, translator.getParameters().get("b0"));
		assertEquals(0L, translator.getParameters().get("b1"));
	}

	@Test
	public void testOverrideNull() throws ParseException {
		Long maxBytesPerPage = null;
		QuerySpecification model = new TableQueryParser("select foo from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, schemaProvider(tableSchema), maxBytesPerPage, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testPLFM_4161() throws ParseException {
		String sql = "select * from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, schemaProvider(schema), userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
	}

	@Test
	public void testDefaultType() throws ParseException {
		// call under test
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		// should default to table.
		assertEquals(TableType.table, query.getTableType());
	}

	@Test
	public void testTableDefaultEtag() throws ParseException {
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		// should default to false
		assertFalse(query.includeEntityEtag());
	}
	
	@Test
	public void testSelectViewWithoutEtag() throws ParseException {
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).includeEntityEtag(null).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
		assertFalse(query.includeEntityEtag());
	}

	@Test
	public void testViewDefaultEtag() throws ParseException {
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).build();
		// should default to false
		assertFalse(query.includeEntityEtag());
	}

	@Test
	public void testSelectViewWithEtag() throws ParseException {
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.includeEntityEtag(true)
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION, ROW_ETAG FROM T123", query.getOutputSQL());
		assertTrue(query.includeEntityEtag());
	}
	
	@Test
	public void testSelectViewWithEtagFalse() throws ParseException {
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.includeEntityEtag(false)
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
		assertFalse(query.includeEntityEtag());
	}

	@Test
	public void testSelectTableWithEtag() throws ParseException {
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
		assertFalse(query.includeEntityEtag());
	}

	@Test
	public void testSelectViewWithEtagAggregate() throws ParseException {
		sql = "select count(*) from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).build();
		assertEquals("SELECT COUNT(*) FROM T123", query.getOutputSQL());
	}

	/**
	 * This is a test for PLFM-4736.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testAliasGroupByOrderBy() throws ParseException {
		sql = "select \"foo\" as \"f\", sum(inttype) as \"i` sum\" from syn123 group by \"f\" order by \"i` sum\" DESC";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(tableSchema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_ AS `f`, SUM(_C888_) AS `i`` sum` FROM T123 GROUP BY `f` ORDER BY `i`` sum` DESC",
				query.getOutputSQL());
	}

	@Test
	public void testCurrentUserFunctionInWhereClause() throws ParseException {
		sql = "select inttype from syn123 where inttype = CURRENT_USER()";
		Map<String, Object> expectedParameters = Collections.singletonMap("b0", userId);
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(tableSchema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C888_, ROW_ID, ROW_VERSION FROM T123 WHERE _C888_ = :b0", query.getOutputSQL());
		assertEquals(expectedParameters, query.getParameters());
	}

	@Test
	public void testCurrentUserFunctionInAggClause() throws ParseException {
		sql = "select COUNT(CURRENT_USER()) from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(tableSchema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(1) FROM T123", query.getOutputSQL());
	}

	@Test
	public void testCurrentUserFunctionInSelectClause() throws ParseException {
		sql = "select CURRENT_USER() from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(tableSchema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT 1, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
	}

	/**
	 * Test added for PLFM-6819.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testQueryUnixTimestamp() throws ParseException {
		ColumnModel createdOn = new ColumnModel();
		createdOn.setColumnType(ColumnType.DATE);
		createdOn.setName("createdOn");
		createdOn.setId("1");
		schema = Arrays.asList(createdOn);

		String sql = "select createdOn, UNIX_TIMESTAMP('2021-06-20 00:00:00')*1000 from syn123 where createdOn > UNIX_TIMESTAMP('2021-06-20 00:00:00')*1000";

		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(schema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C1_, UNIX_TIMESTAMP('2021-06-20 00:00:00')*1000, ROW_ID, ROW_VERSION FROM T123 WHERE _C1_ > UNIX_TIMESTAMP('2021-06-20 00:00:00')*:b0",
				query.getOutputSQL());
		Map<String, Object> expectedParams = new HashMap<>(4);
		expectedParams.put("b0", 1000L);
		assertEquals(expectedParams, query.getParameters());
	}

	@Test
	public void testQueryWithTextMatches() throws ParseException {
		sql = "select foo from syn123 WHERE TEXT_MATCHES('some text')";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(tableSchema))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();

		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE MATCH(ROW_SEARCH_CONTENT) AGAINST(:b0)",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
		assertTrue(query.isIncludeSearch());
	}

	@Test
	public void testTranslateWithJoinWithQueryContext() throws ParseException {
		sql = "select * from syn1 join syn2 on (syn1.id = syn2.id) WHERE syn1.foo is not null";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			QueryTranslator.builder(sql, userId).schemaProvider(schemaProvider(tableSchema)).sqlContext(SqlContext.query)
					.indexDescription(new TableIndexDescription(idAndVersion)).build();
		}).getMessage();
		assertEquals(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);
	}

	@Test
	public void testTranslateWithJoinWithAlowJoinTrue() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		schemaMap.put(IdAndVersion.parse("syn2"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has\"quote")));
		
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 join syn2 on (syn1.foo = syn2.foo) WHERE syn1.bar = 'some text' order by syn1.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C4242_ "
				+ "FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A0._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}
	
	@Test
	public void testTranslateWithJoinWithAlowJoinTrueWithView() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		schemaMap.put(IdAndVersion.parse("syn2"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has\"quote")));

		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new ViewIndexDescription(IdAndVersion.parse("syn2"), TableType.entityview));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 join syn2 on (syn1.foo = syn2.foo) WHERE syn1.bar = 'some text' order by syn1.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C4242_, IFNULL(_A1.ROW_BENEFACTOR,-1) " +
						"FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A0._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithQueryContext() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn123"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));

		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new ViewIndexDescription(IdAndVersion.parse("syn2"), TableType.entityview));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn123 WHERE bar = 'some text'";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123 WHERE _C333_ = :b0",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}

	@Test
	public void testTranslateWithJoinWithAlowJoinTrueWithAlias() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		schemaMap.put(IdAndVersion.parse("syn2"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has\"quote")));
		
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a join syn2 b on (a.foo = b.foo) WHERE a.bar = 'some text' order by a.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.indexDescription(indexDescription).sqlContext(SqlContext.build).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C4242_ "
				+ "FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A0._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}

	@Test
	public void testTranslateWithJoinWithMultipleOfSameTable() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));

		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn1")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a join syn1 b on (a.foo = b.foo) WHERE a.bar = 'some text' order by b.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C333_ "
				+ "FROM T1 _A0 JOIN T1 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A1._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}

	@Test
	public void testTranslateWithJoinWithMultipleOfSameViewWithBuildContext() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));

		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn1"), TableType.entityview));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a join syn1 b on (a.foo = b.foo) WHERE a.bar = 'some text' order by b.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C333_, IFNULL(_A0.ROW_BENEFACTOR,-1) " +
						"FROM T1 _A0 JOIN T1 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A1._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}

	@Test
	public void testTranslateWithLeftJoin() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		schemaMap.put(IdAndVersion.parse("syn2"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has space")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select b.`has space`, sum(a.foo) from syn1 a left join syn2 b on (a.foo = b.foo) group by b.`has space`";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A1._C222_, SUM(_A0._C111_) FROM T1 _A0 LEFT JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ ) GROUP BY _A1._C222_",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithLeftJoinOuter() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		schemaMap.put(IdAndVersion.parse("syn2"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has space")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn1")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a left outer join syn2 b on (a.foo = b.foo)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C222_ FROM T1 _A0 LEFT OUTER JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithRightJoin() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		schemaMap.put(IdAndVersion.parse("syn2"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has space")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a right join syn2 b on (a.foo = b.foo)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C222_ FROM T1 _A0 RIGHT JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithRightJoinOuter() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(IdAndVersion.parse("syn1"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		schemaMap.put(IdAndVersion.parse("syn2"),
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has space")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a right outer join syn2 b on (a.foo = b.foo)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.indexDescription(indexDescription).sqlContext(SqlContext.build).build();
		assertEquals(
				"SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C222_ FROM T1 _A0 RIGHT OUTER JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithIsInfinity() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		idAndVersion = IdAndVersion.parse("syn1");
		schemaMap.put(idAndVersion,
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("doubletype")));

		sql = "select * from syn1 a where isInfinity(doubletype)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C111_," + " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END,"
						+ " ROW_ID, ROW_VERSION FROM T1 "
						+ "WHERE ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithIsNaN() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		idAndVersion = IdAndVersion.parse("syn1");
		schemaMap.put(idAndVersion,
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("doubletype")));

		sql = "select * from syn1 a where isNaN(doubletype)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C111_," + " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END,"
						+ " ROW_ID, ROW_VERSION FROM T1 "
						+ "WHERE ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithDoubleDefiningSql() throws ParseException {

		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(viewId, Arrays.asList(columnNameToModelMap.get("doubletype")));
		schemaMap.put(materializedViewId, Arrays.asList(columnNameToModelMap.get("doubletype")));

		List<IndexDescription> dependencies = Arrays.asList(new ViewIndexDescription(viewId, TableType.dataset));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select doubletype from syn1";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _C777_, IFNULL(ROW_BENEFACTOR,-1) FROM T1", query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithDoubleQuerySql() throws ParseException {
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(viewId, Arrays.asList(columnNameToModelMap.get("doubletype")));
		schemaMap.put(materializedViewId, Arrays.asList(columnNameToModelMap.get("doubletype")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(viewId, TableType.dataset));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		
		// this is a query against a materialized view.
		sql = "select * from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END, ROW_ID, ROW_VERSION FROM T123",
				query.getOutputSQL());
	}
	
	@Test
	public void testGetSchemaOfSelect() throws ParseException {
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		idAndVersion = IdAndVersion.parse("syn1");
		schemaMap.put(idAndVersion,
				Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("doubletype")));

		sql = "select * from syn1 a where isNaN(doubletype)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		// call under test
		List<ColumnModel> schema = query.getSchemaOfSelect();
		List<ColumnModel> expected = Arrays.asList(
				new ColumnModel().setName("a.foo").setColumnType(ColumnType.STRING).setId(null).setMaximumSize(50L),
				new ColumnModel().setName("a.doubletype").setColumnType(ColumnType.DOUBLE).setId(null)
		);
		assertEquals(expected, schema);
	}
	
	@Test
	public void testJoinViewAndTableWithDepenenciesOutOfOrder() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(viewId, Arrays.asList(columnNameToModelMap.get("inttype")));
		schemaMap.put(tableId, Arrays.asList(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order. 
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on a.inttype = b.inttype";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C888_, _A1._C888_, IFNULL(_A0.ROW_BENEFACTOR,-1) FROM T1 _A0 JOIN T2 _A1 ON _A0._C888_ = _A1._C888_",
				query.getOutputSQL());
	}
	
	@Test
	public void testJoinSameViewMultipleTimes() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(viewId, Arrays.asList(columnNameToModelMap.get("inttype")));
		schemaMap.put(tableId, Arrays.asList(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order. 
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on (a.inttype = b.inttype) join syn1 c on (b.inttype = c.inttype)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C888_, _A1._C888_, _A2._C888_, IFNULL(_A0.ROW_BENEFACTOR,-1) FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C888_ = _A1._C888_ ) JOIN T1 _A2 ON ( _A1._C888_ = _A2._C888_ )",
				query.getOutputSQL());
	}
	
	@Test
	public void testBuildMaterializedViewWithViewDependencyAndGroupByInDefiningSql() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(viewId, Arrays.asList(columnNameToModelMap.get("foo"),columnNameToModelMap.get("bar")));

		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select foo, sum(bar) from syn1 group by foo";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
			.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		}).getMessage();
		assertEquals(TableConstants.DEFINING_SQL_WITH_GROUP_BY_ERROR, message);
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithWhereColumnNotFound() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(viewId, Arrays.asList(columnNameToModelMap.get("inttype")));
		schemaMap.put(tableId, Arrays.asList(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order. 
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on (a.inttype = b.inttype) where a.inttypeWrong = 123";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
			.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		}).getLocalizedMessage();
		assertEquals("Column does not exist: a.inttypeWrong", message);
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithJoinColumnNotFound() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(viewId, Arrays.asList(columnNameToModelMap.get("inttype")));
		schemaMap.put(tableId, Arrays.asList(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order. 
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on (a.inttypeWrong = b.inttype)";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
			.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		}).getLocalizedMessage();
		assertEquals("Column does not exist: a.inttypeWrong", message);
	}

	@Test
	public void testTranslateWithMaterializedViewWithComplexCondition() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		Map<IdAndVersion, List<ColumnModel>> schemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		schemaMap.put(viewId, Arrays.asList(columnNameToModelMap.get("inttype")));
		schemaMap.put(tableId, Arrays.asList(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order.
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on (a.inttype = b.inttype and a.inttype > 12)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(new TestSchemaProvider(schemaMap))
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C888_, _A1._C888_, IFNULL(_A0.ROW_BENEFACTOR,-1) FROM T1 _A0 " +
						"JOIN T2 _A1 ON ( _A0._C888_ = _A1._C888_ AND _A0._C888_ > :b0 )",
				query.getOutputSQL());
		Map<String, Object> expectedParams = new HashMap<>(4);
		expectedParams.put("b0", 12L);
		assertEquals(expectedParams, query.getParameters());
	}

	/**
	 * Helper to create a schema provider for the given schema.
	 * 
	 * @param schema
	 * @return
	 */
	SchemaProvider schemaProvider(List<ColumnModel> schema) {
		return (IdAndVersion tableId) -> {
			return schema;
		};
	}

}

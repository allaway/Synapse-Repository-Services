package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.util.FacetRequestColumnModel;
import org.sagebionetworks.table.query.util.FacetUtils;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;

public class FacetTransformerRange implements FacetTransformer {
	public static final String MIN_ALIAS = "minimum";
	public static final String MAX_ALIAS = "maximum";
	
	private String columnName;
	private List<FacetRequestColumnModel> facets;
	private String selectedMin;
	private String selectedMax;
	
	private QueryTranslator generatedFacetSqlQuery;
	
	public FacetTransformerRange(String columnName, List<FacetRequestColumnModel> facets, TableExpression originalQuery, TranslationDependencies dependencies, String selectedMin, String selectedMax){
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(facets, "facets");
		ValidateArgument.required(originalQuery, "originalQuery");
		this.columnName = columnName;
		this.facets = facets;
		this.selectedMin = selectedMin;
		this.selectedMax = selectedMax;
		this.generatedFacetSqlQuery = generateFacetSqlQuery(originalQuery, dependencies);
	}
	
	@Override
	public String getColumnName() {
		return this.columnName;
	}
	
	@Override
	public QueryTranslator getFacetSqlQuery(){
		return this.generatedFacetSqlQuery;
	}
	
	/**
	 * Creates a new SQL query for finding the minimum and maximum values of a faceted column
	 * @return the generated SQL query represented by SqlQuery
	 */
	private QueryTranslator generateFacetSqlQuery(TableExpression originalQuery, TranslationDependencies dependencies) {
		
		StringBuilder builder = new StringBuilder("SELECT MIN(");
		builder.append("\"");
		builder.append(columnName);
		builder.append("\"");
		builder.append(") as ");
		builder.append(MIN_ALIAS);
		builder.append(", MAX(");
		builder.append("\"");
		builder.append(columnName);
		builder.append("\"");
		builder.append(") as ");
		builder.append(MAX_ALIAS);
		builder.append(" ");
		builder.append(originalQuery.getFromClause().toSql());
		String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(facets, columnName);
		SqlElementUtils.appendCombinedWhereClauseToStringBuilder(builder, facetSearchConditionString, originalQuery.getWhereClause());
		
		return QueryTranslator.builder(builder.toString(), dependencies).build();
	}
	
	@Override
	public FacetColumnResult translateToResult(RowSet rowSet) {
		ValidateArgument.required(rowSet, "rowSet");
		List<SelectColumn> headers = rowSet.getHeaders();
		//check for expected headers
		if(headers.size() != 2 || !headers.get(0).getName().equals(MIN_ALIAS) || !headers.get(1).getName().equals(MAX_ALIAS)){
			throw new IllegalArgumentException("The RowSet's headers did not contain the expected column names");
		}
		
		List<Row> rows = rowSet.getRows();
		//should only have one row
		if(rows.size() != 1){ 
			throw new IllegalArgumentException("Expected RowSet to only have one row");
		}
		
		Row row = rows.get(0);
		
		FacetColumnResultRange result = new FacetColumnResultRange();
		result.setColumnName(this.columnName);
		result.setFacetType(FacetType.range);
		List<String> values =  row.getValues();
		result.setColumnMin(values.get(0));
		result.setColumnMax(values.get(1));
		result.setSelectedMin(this.selectedMin);
		result.setSelectedMax(this.selectedMax);
		
		return result;
	}

}

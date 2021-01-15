/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.jdbc;

import io.inversion.*;
import io.inversion.rql.*;
import io.inversion.rql.Order.Sort;
import io.inversion.utils.Rows;
import io.inversion.utils.Utils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Composes and executes a SQL SELECT based on supplied RQL <code>Terms</code>.
 */
public class SqlQuery<D extends Db> extends Query<SqlQuery, D, Select<Select<Select, SqlQuery>, SqlQuery>, From<From<From, SqlQuery>, SqlQuery>, Where<Where<Where, SqlQuery>, SqlQuery>, Group<Group<Group, SqlQuery>, SqlQuery>, Order<Order<Order, SqlQuery>, SqlQuery>, Page<Page<Page, SqlQuery>, SqlQuery>> {

    protected char stringQuote = '\'';
    protected char columnQuote = '"';

    String type = null;

    LinkedHashMap<String, Term> joins;

//    public SqlQuery() {
//
//    }

    public SqlQuery(D db, Collection table, List<Term> terms) {
        super(db, table, terms, "_query");

        //-- this is done in the super call but this second call 
        //-- is required to set the columnQuote after member variable
        //-- initialization happens AFTER the super call.
        withDb(db);
    }

    protected boolean addTerm(String token, Term term) {

        if (term.hasToken("eq")) {
            String name = term.getToken(0);

            //TODO document this
            if (name.endsWith(".select"))
                return true;
        }

        if (term.hasToken("join")) {
            if (joins == null)
                joins = new LinkedHashMap<>();

            //a map is used to avoid adding duplicate joins
            String key = term.toString();
            if (!joins.containsKey(key)) {
                joins.put(key, term);
            }
            return true;
        }

        return super.addTerm(token, term);
    }

    @Override
    public Results doSelect() throws ApiException {
        JdbcDb db  = (JdbcDb) getDb();
        String sql = getPreparedStmt();

        Results results = new Results(this);
        List    values  = getColValues();

        //-- for test cases and query explain
        String debug = getClass().getSimpleName() + " " + getType() + ": " + sql + " args=" + values;
        debug = debug.replaceAll("\r", "");
        debug = debug.replaceAll("\n", " ");
        debug = debug.replaceAll(" +", " ");
        Chain.debug(debug);
        results.withTestQuery(debug);

        if (!isDryRun()) {
            Connection conn = db.getConnection();
            //-- prepared statement variables are computing during the
            //-- generation of the prepared statement above

            try {
                Rows rows      = JdbcUtils.selectRows(conn, sql, values);
                results.withRows(rows);

                boolean usesAfter = false;

                //-- adds support for index based pagination instead of offset pagination if page params were not supplied
                if(!getPage().isPaginated()){
                    if(rows.size() > 0 && collection != null) {
                        Index pk = collection.getPrimaryIndex();
                        if (pk != null && pk.size() == 1) {
                            List<Sort> sorts = getOrder().getSorts();
                            if (sorts.size() > 0) {
                                String pkCol = pk.getColumnName(0);
                                Sort   last  = sorts.get(sorts.size() - 1);
                                if (last.getProperty().equalsIgnoreCase(pk.getColumnName(0))) {
                                    String lastPk = collection.encodeDbKey(rows.get(rows.size()-1));
                                    if(lastPk != null){
                                        usesAfter = true;
                                        if(last.isAsc())
                                            results.withNext(Term.term(null, "gt", pk.getColumnName(0), lastPk));
                                        else
                                            results.withNext(Term.term(null, "lt", pk.getColumnName(0), lastPk));
                                    }
                                }
                            }
                        }
                    }
                }

                if(!usesAfter){
                    int foundRows = rows.size();
                    int limit     = getPage().getLimit();
                    int page      = getPage().getOffset();

                    //-- don't query for total row count if the DB returned fewer that
                    //-- the max number of rows on the first page...foundRows must be
                    //-- number of rows we just found
                    boolean needsPaging = page > 1 || foundRows == limit;
                    if (needsPaging) {
                        if (Chain.peek().get("foundRows") == null && Chain.first().getRequest().isMethod("GET")) {
                            if (rows.size() == 0) {
                                foundRows = 0;
                            } else {

                                foundRows = queryFoundRows(conn, sql, values);
                            }

                            Chain.peek().put("foundRows", foundRows);
                        }
                    }
                    results.withFoundRows(foundRows);
                }

            } catch (Exception ex) {
                //System.out.println(sql);
                ex.printStackTrace();
                throw ApiException.new500InternalServerError(ex);
            }
        }

        return results;
    }

    public String getPreparedStmt() {
        return toSql(true);
    }

    public String getDynamicStmt() {
        return toSql(false);
    }

    protected String toSql(boolean preparedStmt) {
        clearValues();
        Parts parts = new Parts();

        printInitialSelect(parts);
        printTermsSelect(parts, preparedStmt);
        //printJoins(parts, joins);
        printWhereClause(parts, getWhere().getFilters(), preparedStmt);
        printGroupClause(parts, getGroup().getGroupBy());
        printOrderClause(parts, getOrder());
        printLimitClause(parts, getPage().getOffset(), getPage().getLimit());

        String sql = printSql(parts);
        return sql;
    }

    protected String printSql(Parts parts) {
        //--compose the final statement
        String buff = parts.select;

        buff += " \r\n" + parts.from;

        if (parts.where != null)
            buff += " \r\n" + parts.where;

        if (parts.select.toLowerCase().startsWith("select ")) {
            if (parts.group != null)
                buff += " \r\n" + parts.group;

            if (parts.order != null)
                buff += " \r\n" + parts.order;

            if (parts.limit != null)
                buff += " \r\n" + parts.limit;
        }

        String sql = buff;
        return sql;
    }

    protected String printInitialSelect(Parts parts) {
        String initialSelect = (String) find("_query", 0);

        if (Utils.empty(initialSelect)) {
            String  expression = getFrom().getSubquery();

            String  alias      = quoteCol(getFrom().getAlias());
            boolean distinct   = getSelect().isDistinct();

            if (expression != null) {
                initialSelect = " SELECT " + (distinct ? "DISTINCT " : "") + alias + ".* FROM (" + expression + ")";
            } else {

                String table = getFrom().getTable();
                if(Utils.empty(table))
                    throw ApiException.new400BadRequest("Your requested url '{}' could not be mapped to a table to query.", Chain.peek().getRequest().getUrl());

                expression = quoteCol(getFrom().getTable());
                initialSelect = " SELECT " + (distinct ? "DISTINCT " : "") + alias + ".* FROM " + expression;
            }

            if (!expression.equals(alias)) {
                initialSelect += " AS " + alias;
            }
        }

        parts.withSql(initialSelect);
        return initialSelect;
    }

    protected String printTermsSelect(Parts parts, boolean preparedStmt) {
        StringBuilder cols = new StringBuilder();

        List<Term> terms = getSelect().getAllColumns();
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            if (term.hasToken("as")) {
                Term function = term.getTerm(0);
                cols.append(" ").append(printTerm(function, null, preparedStmt));

                String colName = term.getToken(1);
                if (!(Utils.empty(colName) || colName.contains("$$$ANON"))) {
                    //TODO: validate if this mysql special case is required...will require changing tests if removed
                    if (db == null || db.isType("mysql"))
                        cols.append(" AS ").append(asString(colName));
                    else
                        cols.append(" AS ").append(quoteCol(colName));
                }
            } else if (!term.getToken().contains(".")) {
                cols.append(" ").append(printCol(term.getToken()));
            }

            if (i < terms.size() - 1)
                cols.append(", ");
        }

        if (cols.length() > 0) {
            boolean aggregate = find("sum", "min", "max", "count", "function", "aggregate", "distinct") != null;
            boolean restrictCols = find("include") != null || aggregate;

            if (db == null || db.isType("mysql")) {
                //-- this a special case for legacy mysql that does not require grouping each column when aggregating
                restrictCols = find("include") != null;
            }

            int star = parts.select.lastIndexOf("* ");
            if (restrictCols && star > 0) {
                //inserts the select list before the *
                int    idx       = parts.select.substring(0, star).indexOf(" ");
                String newSelect = parts.select.substring(0, idx) + cols + parts.select.substring(star + 1);
                parts.select = newSelect;
            } else {
                String select = parts.select.trim();
                if (!select.toLowerCase().endsWith("select"))
                    select += ", ";
                select += cols;

                parts.select = select;
            }
        }

        if (getSelect().isDistinct() && !parts.select.toLowerCase().contains("distinct")) {
            int idx = parts.select.toLowerCase().indexOf("select") + 6;
            parts.select = parts.select.substring(0, idx) + " DISTINCT " + parts.select.substring(idx);
        }

        if (getPage().isPaginated() && Chain.peek() != null && Chain.peek().get("foundRows") == null && "mysql".equalsIgnoreCase(getType()) && parts.select.toLowerCase().trim().startsWith("select")) {
            int idx = parts.select.toLowerCase().indexOf("select") + 6;
            parts.select = parts.select.substring(0, idx) + " SQL_CALC_FOUND_ROWS " + parts.select.substring(idx);
        }

        return parts.select;
    }

    protected String printJoins(Parts parts, LinkedHashMap<String, Term> joins) {
        if (joins != null) {
            for (Entry<String, Term> joinTerm : joins.entrySet()) {
                Term join = joinTerm.getValue();

                StringBuilder where = new StringBuilder();

                for (int j = 2; j < join.size(); j += 4)//the first token is the related name, the second token is the table alias
                {
                    if (j > 2)
                        where.append(" AND ");
                    where.append(quoteCol(join.getToken(j))).append(".").append(quoteCol(join.getToken(j + 1))).append(" = ").append(quoteCol(join.getToken(j + 2))).append(".").append(quoteCol(join.getToken(j + 3)));
                }

                if (where != null) {
                    where = new StringBuilder("(" + where + ")");

                    if (Utils.empty(parts.where))
                        parts.where = " WHERE " + where;
                    else
                        parts.where += " AND " + where;
                }
            }
        }
        return parts.where;
    }

    //   protected Term findJoinTerm(Term term)
    //   {
    //      if (!this.joins.isEmpty())
    //      {
    //         String s = term.getToken(0);
    //         if (s.contains("."))
    //         {
    //            String[] arr = s.split("\\.");
    //            for (Entry<String, Term> joinTerm : joins.entrySet())
    //            {
    //               Term join = joinTerm.getValue();
    //               if (join.getToken(1).equals(arr[0]))
    //               {
    //                  return join;
    //               }
    //            }
    //         }
    //      }
    //      return null;
    //   }

    //   protected String printJoinSubselect(Term term, String col, boolean preparedStmt, Term joinTerm)
    //   {
    //      // (`Location`.`id` not in(select locationId from LocationTag where `content` LIKE '%froster%'))
    //
    //      String t1 = joinTerm.getToken(2);
    //      String c1 = joinTerm.getToken(3);
    //
    //      String t2 = joinTerm.getToken(0);
    //      String c2 = joinTerm.getToken(5);
    //
    //      String alias = joinTerm.getToken(1);
    //
    //      boolean negation = term.hasToken("ne", "nw", "wo");
    //
    //      String in = " in (select ";
    //      if (negation)
    //      {
    //         in = " not in (select ";
    //
    //         // need to swap to the positive form since it will be a "not in"
    //         Map<String, String> ops = new HashMap<>();
    //         ops.put("ne", "eq");
    //         ops.put("nw", "w");
    //         ops.put("wo", "w");
    //         term.withToken(ops.get(term.getToken()));
    //      }
    //
    //      String where = printTerm(term, col, preparedStmt);
    //      String subselect = quoteCol(t1) + "." + quoteCol(c1) + in + quoteCol(c2) + " from " + quoteCol(t2) + " " + quoteCol(alias) + " where " + where + ") ";
    //
    //      return subselect;
    //   }

    protected String printWhereClause(Parts parts, List<Term> terms, boolean preparedStmt) {
        for (Term term : terms) {
            String where = printTerm(term, null, preparedStmt);
            if (where != null) {
                if (Utils.empty(parts.where))
                    parts.where = " WHERE " + where;
                else
                    parts.where += " AND " + where;
            }
        }
        return parts.where;
    }

    protected String printGroupClause(Parts parts, List<String> groupBy) {
        if (groupBy.size() > 0) {

            StringBuilder group = new StringBuilder(parts.group != null ? parts.group : "GROUP BY ");

            for (String column : groupBy) {
                if (!Utils.endsWith(group, "GROUP BY "))
                    group.append(", ");
                group.append(printCol(column));
            }
            parts.group = group.toString();
        }
        return parts.group;
    }

    protected String printOrderClause(Parts parts, Order order) {

        List<Sort> sorts = order.getSorts();

        //-- before printing the "order by" statement, if the users did not supply
        //-- any sort terms but the primary index key is being selected, sort on the
        //-- primary index.
        //-- TODO: can this be moved into the Order builder?

        if (sorts.isEmpty()) {
            sorts = getDefaultSorts(parts);
            order.withSorts(sorts);
        }

        for (Sort sort : sorts) {
            //-- now setup the "ORDER BY" clause based on the
            //-- "sort" parameter.  Multiple sorts can be
            //-- comma separated and a leading '-' indicates
            //-- descending sort for that field
            //--
            //-- ex: "sort=firstName,-age"

            if (parts.order == null)
                parts.order = "ORDER BY ";

            if (!parts.order.endsWith("ORDER BY "))
                parts.order += ", ";

            parts.order += printCol(sort.getProperty()) + (sort.isAsc() ? " ASC" : " DESC");
        }
        return parts.order;
    }

    protected List<Sort> getDefaultSorts(Parts parts) {
        List<Sort> sorts = new ArrayList<>();

        boolean wildcard = parts.select.contains("* ") //
                || parts.select.contains("*,");//this trailing space or comma is important because otherwise this would incorrectly match "COUNT(*)"

        if (collection != null && collection.getPrimaryIndex() != null) {
            for (String pkCol : collection.getPrimaryIndex().getColumnNames()) {
                if (wildcard || parts.select.contains(quoteCol(pkCol))) {
                    Sort sort = new Sort(pkCol, true);
                    sorts.add(sort);
                }
            }
        }

        //-- in this case the primaryIndex cols are not included in the select
        //-- meaning this must be some time of aggregate...so we will do a
        //-- full sort on all selected columns

        //-- TODO: make test case for this
        if (sorts.size() == 0) {
            boolean hasCol = false;
            for (Term term : select.getAllColumns()) {
                if (term.isLeaf() || term.hasToken("as") && term.getTerm(0).isLeaf()) {
                    hasCol = true;
                    break;
                }
            }

            if (!hasCol)
                return sorts;

            Collection coll = getCollection();
            if (coll != null) {
                for (Property prop : coll.getProperties()) {
                    if (parts.select.contains(quoteCol(prop.getColumnName())))
                        sorts.add(new Sort(prop.getColumnName(), true));
                }
            } else {
                for (String col : getSelect().getIncludeColumns()) {
                    sorts.add(new Sort(col, true));
                }
            }
        }

        return sorts;
    }

    protected String printLimitClause(Parts parts, int offset, int limit) {
        if (Utils.empty(parts.order))
            return "";

        String s = null;
        if (limit >= 0 || offset >= 0) {
            if (db == null || getDb().isType("mysql")) {
                s = "LIMIT ";
                if (offset > 0)
                    s += offset;

                if (limit >= 0) {
                    if (!s.endsWith("LIMIT "))
                        s += ", ";

                    s += limit;
                }
            } else if (getDb().isType("sqlserver")) {
                //https://docs.microsoft.com/en-us/sql/t-sql/queries/select-order-by-clause-transact-sql?view=sql-server-2017#Offset
                //OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY

                s = "";
                if (offset >= 0)
                    s += " OFFSET " + offset + " ROWS ";

                if (limit >= 0)
                    s += " FETCH NEXT " + limit + " ROWS ONLY ";
            } else {
                s = "";

                if (limit >= 0)
                    s += " LIMIT " + limit;

                if (offset >= 0)
                    s += " OFFSET " + offset;
            }
        }

        parts.limit = s;
        return s;
    }

    protected int queryFoundRows(Connection conn, String sql, List values) throws Exception {
        int foundRows;
        if (db.isType("mysql")) {
            sql = "SELECT FOUND_ROWS()";
            foundRows = JdbcUtils.selectInt(conn, sql);
        } else {
            if (sql.indexOf("LIMIT ") > 0)
                sql = sql.substring(0, sql.lastIndexOf("LIMIT "));

            if (sql.indexOf("OFFSET ") > 0)
                sql = sql.substring(0, sql.lastIndexOf("OFFSET "));

            if (sql.indexOf("ORDER BY ") > 0)
                sql = sql.substring(0, sql.lastIndexOf("ORDER BY "));

            sql = "SELECT count(1) FROM ( " + sql + " ) as q";

            foundRows = JdbcUtils.selectInt(conn, sql, values);
        }
        return foundRows;
    }

    protected String printTerm(Term term, String col, boolean preparedStmt) {
        if (term.isLeaf()) {
            String token = term.getToken();

            String value;
            if (isCol(term)) {
                value = printCol(token);
            } else if (isBool(term)) {
                value = asBool(token);
            } else if (isNum(term)) {
                value = asNum(token);
            } else {
                value = asString(term);
            }
            return value;
        }

        for (Term child : term.getTerms()) {
            if (isCol(child)) {
                col = child.token;
                break;
            }
        }

        List<String> dynamicSqlChildText = new ArrayList<>();
        for (Term t : term.getTerms()) {
            dynamicSqlChildText.add(printTerm(t, col, preparedStmt));
        }

        List<String> preparedStmtChildText = new ArrayList(dynamicSqlChildText);

        //allows for callers to substitute callable statement "?"s
        //and/or to account for data type conversion
        if (preparedStmt) {
            for (int i = 0; i < term.size(); i++) {
                Term t = term.getTerm(i);
                if (t.isLeaf()) {
                    String val = preparedStmtChildText.get(i);
                    if (val.charAt(0) != columnQuote) {
                        val = Utils.dequote(val);//go back to the unprinted/quoted version
                        preparedStmtChildText.set(i, replace(term, t, i, col, val));
                    }
                }
            }
        }

        return printExpression(term, dynamicSqlChildText, preparedStmtChildText);

    }

    /**
     * Override to handle printing additional functions or to change the
     * way a specific function is printed.
     *
     * @param term                  the term to print
     * @param dynamicSqlChildText   child tokens printed as dynamic sql
     * @param preparedStmtChildText child tokens printed as prepared statements
     * @return the sql statement for term
     */
    protected String printExpression(Term term, List<String> dynamicSqlChildText, List<String> preparedStmtChildText) {
        String token = term.getToken();

        StringBuilder sql = new StringBuilder();

        if (term.hasToken("eq", "ne", "like", "w", "sw", "ew", "wo")) {
            boolean negation = term.hasToken("ne", "nw", "wo");

            if (term.size() > 2 || negation)
                sql.append("(");

            if (negation)
                sql.append("NOT (");

            String string0 = preparedStmtChildText.get(0);

            for (int i = 1; i < term.size(); i++) {
                String stringI = preparedStmtChildText.get(i);
                if ("null".equalsIgnoreCase(stringI)) {
                    sql.append(string0).append(" IS NULL ");
                } else {
                    boolean wildcard = dynamicSqlChildText.get(i).indexOf('%') >= 0;

                    if (wildcard) {
                        //this postgres engine uses "ilike" instead of "like" for case insensitive matching
                        if (getDb().isType("h2", "postgres", "redshift"))
                            sql.append(string0).append(" ILIKE ").append(stringI);
                        else {
                            sql.append(string0).append(" LIKE ").append(stringI);

                            //-- the escape character in sqlserver must be intentionally
                            //-- identified in the sql
                            //-- @see https://docs.microsoft.com/en-us/sql/connect/jdbc/using-sql-escape-sequences?view=sql-server-ver15
                            if (getDb().isType("sqlserver") //
                                    && dynamicSqlChildText.get(i).indexOf('\\') >= 0) {
                                sql.append(" {escape '\\'}");
                            }
                        }
                    } else {
                        sql.append(string0).append(" = ").append(stringI);
                    }
                }

                if (i < term.size() - 1)
                    sql.append(" OR ");
            }

            if (term.size() > 2 || negation)
                sql.append(")");

            if (negation)
                sql.append(")");
        } else if ("nn".equalsIgnoreCase(token)) {
            sql.append(concatAll(" AND ", " IS NOT NULL", preparedStmtChildText));
        } else if ("emp".equalsIgnoreCase(token)) {
            sql.append("(").append(preparedStmtChildText.get(0)).append(" IS NULL OR ").append(preparedStmtChildText.get(0)).append(" = ").append(asString("")).append(")");
        } else if ("nemp".equalsIgnoreCase(token)) {
            sql.append("(").append(preparedStmtChildText.get(0)).append(" IS NOT NULL AND ").append(preparedStmtChildText.get(0)).append(" != ").append(asString("")).append(")");
        } else if ("n".equalsIgnoreCase(token)) {
            sql.append(concatAll(" AND ", " IS NULL", preparedStmtChildText));
        } else if ("lt".equalsIgnoreCase(token)) {
            sql.append(preparedStmtChildText.get(0)).append(" < ").append(preparedStmtChildText.get(1));
        } else if ("le".equalsIgnoreCase(token)) {
            sql.append(preparedStmtChildText.get(0)).append(" <= ").append(preparedStmtChildText.get(1));
        } else if ("gt".equalsIgnoreCase(token)) {
            sql.append(preparedStmtChildText.get(0)).append(" > ").append(preparedStmtChildText.get(1));
        } else if ("ge".equalsIgnoreCase(token)) {
            sql.append(preparedStmtChildText.get(0)).append(" >= ").append(preparedStmtChildText.get(1));
        } else if ("in".equalsIgnoreCase(token) || "out".equalsIgnoreCase(token)) {
            sql.append(preparedStmtChildText.get(0));

            if ("out".equalsIgnoreCase(token))
                sql.append(" NOT");

            sql.append(" IN(");
            for (int i = 1; i < preparedStmtChildText.size(); i++) {
                sql.append(preparedStmtChildText.get(i));
                if (i < preparedStmtChildText.size() - 1)
                    sql.append(", ");
            }
            sql.append(")");
        } else if ("if".equalsIgnoreCase(token)) {
            if (db == null || db.isType("mysql")) {
                sql.append("IF(").append(preparedStmtChildText.get(0)).append(", ").append(preparedStmtChildText.get(1)).append(", ").append(preparedStmtChildText.get(2)).append(")");
            } else {
                sql.append("CASE WHEN ").append(preparedStmtChildText.get(0)).append(" THEN ").append(preparedStmtChildText.get(1)).append(" ELSE ").append(preparedStmtChildText.get(2)).append(" END");
            }
        } else if ("and".equalsIgnoreCase(token) || "or".equalsIgnoreCase(token)) {
            sql.append("(");
            for (int i = 0; i < preparedStmtChildText.size(); i++) {
                sql.append(preparedStmtChildText.get(i).trim());
                if (i < preparedStmtChildText.size() - 1)
                    sql.append(" ").append(token.toUpperCase()).append(" ");
            }
            sql.append(")");
        } else if ("not".equalsIgnoreCase(token)) {
            sql.append("NOT (");
            for (int i = 0; i < preparedStmtChildText.size(); i++) {
                sql.append(preparedStmtChildText.get(i).trim());
                if (i < preparedStmtChildText.size() - 1)
                    sql.append(" ");
            }
            sql.append(")");
        } else if ("count".equalsIgnoreCase(token)) {
            //String s = "COUNT (1)";
            //sql.append(s);

            String pt   = printTableAlias() + ".*";
            String acol = preparedStmtChildText.get(0);
            if (pt.equals(acol))//reset count(table.*) to count(*)
                acol = "*";

            String s = token.toUpperCase() + "(" + acol + ")";
            sql.append(s);

        } else if ("distinct".equalsIgnoreCase(token)) {
            //TODO: what do you do with this as a function????
        } else if ("sum".equalsIgnoreCase(token) || "min".equalsIgnoreCase(token) || "max".equalsIgnoreCase(token)) {
            String acol = preparedStmtChildText.get(0);
            String s    = token.toUpperCase() + "(" + acol + ")";
            sql.append(s);
        } else if (Utils.in(token.toLowerCase(), "_exists", "_notexists")) {
            if ("_notexists".equalsIgnoreCase(token))
                sql.append("NOT ");
            sql.append("EXISTS (SELECT 1 FROM ");

            ///-----

            //the RQL comes in looking like like eq(relationship.column,value), and Where.java
            //transforms it to eq(~~relTbl_relationship.column,value)

            String relName = term.getTerm(0).getTerm(0).getToken();
            relName = relName.substring(0, relName.indexOf("."));
            relName = relName.substring(relName.indexOf("_") + 1);

            Relationship rel = collection.getRelationship(relName);

            if (rel == null)
                throw ApiException.new400BadRequest("Unable to find relationship for term '{}'", term);

            String relTbl   = rel.getRelated().getTableName();
            String relAlias = "~~relTbl_" + relTbl;

            String lnkTbl   = rel.getFk1Col1().getCollection().getTableName();
            String lnkAlias = "~~lnkTbl_" + lnkTbl;

            sql.append(quoteCol(relTbl)).append(" ").append(quoteCol(relAlias));

            if (rel.isManyToMany()) {
                sql.append(", ").append(quoteCol(lnkTbl)).append(" ").append(quoteCol(lnkAlias));
            }

            ///-----

            sql.append(" WHERE ");

            Index fk1 = rel.getFkIndex1();

            if (rel.isManyToOne()) {
                for (int i = 0; i < fk1.size(); i++) {
                    Property prop   = fk1.getProperty(i);
                    String   pkName = prop.getPk().getColumnName();
                    String   fkName = prop.getColumnName();
                    sql.append(printTableAlias()).append(".").append(quoteCol(fkName)).append(" = ").append(quoteCol(relAlias)).append(".").append(quoteCol(pkName));

                    if (i < fk1.size() - 1)
                        sql.append(" AND ");
                }
            } else if (rel.isOneToMany()) {
                for (int i = 0; i < fk1.size(); i++) {
                    Property prop   = fk1.getProperty(i);
                    String   pkName = prop.getPk().getColumnName();
                    String   fkName = prop.getColumnName();
                    sql.append(printTableAlias()).append(".").append(quoteCol(pkName)).append(" = ").append(quoteCol(relAlias)).append(".").append(quoteCol(fkName));
                    if (i < fk1.size() - 1)
                        sql.append(" AND ");
                }
            } else if (rel.isManyToMany()) {
                for (int i = 0; i < fk1.size(); i++) {
                    Property prop   = fk1.getProperty(i);
                    String   pkName = prop.getPk().getColumnName();
                    String   fkName = prop.getColumnName();
                    sql.append(printTableAlias()).append(".").append(quoteCol(pkName)).append(" = ").append(quoteCol(lnkAlias)).append(".").append(quoteCol(fkName));
                    sql.append(" AND ");
                }

                Index fk2 = rel.getFkIndex2();
                for (int i = 0; i < fk2.size(); i++) {
                    Property prop   = fk2.getProperty(i);
                    String   pkName = prop.getPk().getColumnName();
                    String   fkName = prop.getColumnName();
                    sql.append(quoteCol(lnkAlias)).append(".").append(quoteCol(fkName)).append(" = ").append(quoteCol(relAlias)).append(".").append(quoteCol(pkName));

                    if (i < fk2.size() - 1)
                        sql.append(" AND ");
                }
            }

            sql.append(" AND ");
            for (int i = 0; i < preparedStmtChildText.size(); i++) {
                sql.append(preparedStmtChildText.get(i));
                if (i < preparedStmtChildText.size() - 1)
                    sql.append(" AND ");
            }
            sql.append(")");
        } else if ("miles".equalsIgnoreCase(token)) {
            sql.append("point(").append(preparedStmtChildText.get(0)).append(",").append(preparedStmtChildText.get(1)).append(") <@> point(").append(preparedStmtChildText.get(2)).append(",").append(preparedStmtChildText.get(3)).append(")");
        } else {
            throw new RuntimeException("Unable to parse: " + term);
        }

        return sql.toString();
    }

    protected String replace(Term parent, Term leaf, int index, String col, String val) {
        if (val == null || val.trim().equalsIgnoreCase("null"))
            return "NULL";

        //      if("*".equals(val))
        //         return val;

        if (parent.hasToken("if") && index > 0) {
            if (isBool(leaf))
                return asBool(val);

            if (isNum(leaf))
                return val;
        }

        withColValue(col, val);
        return asVariableName(values.size() - 1);
    }

    protected String concatAll(String connector, String function, List strings) {
        StringBuilder buff = new StringBuilder((strings.size() > 1 ? "(" : ""));
        for (int i = 0; i < strings.size(); i++) {
            buff.append(strings.get(i)).append(function);
            if (i < strings.size() - 1)
                buff.append(connector);
        }
        if (strings.size() > 1)
            buff.append(")");

        return buff.toString();
    }

    protected boolean isCol(Term term) {
        if (!term.isLeaf())
            return false; //this is a function

        if (term.getQuote() == '"')//" is always the term identifier quote
            return true;

        if (term.getQuote() == '\'')
            return false; //this a string as specified by the user in the parsed rql

        if (isBool(term))
            return false;

        if (isNum(term))
            return false;

        if (collection != null && collection.getProperty(term.getToken()) != null)
            return true;

        return term.getParent() != null && term.getParent().indexOf(term) == 0;
    }

    public String quoteCol(String str) {
        StringBuilder buff  = new StringBuilder();
        String[]      parts = str.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if ("*".equals(parts[i])) {
                buff.append(parts[i]);
            } else {
                if (parts[i].startsWith("~~relTbl_")) {
                    String relName = parts[i];
                    relName = relName.substring(relName.indexOf("_") + 1);

                    Relationship rel = collection.getRelationship(relName);
                    if (rel != null) {
                        parts[i] = "~~relTbl_" + rel.getRelated().getTableName();
                    }
                }

                buff.append(columnQuote).append(parts[i]).append(columnQuote);
            }

            if (i < parts.length - 1)
                buff.append(".");
        }

        return buff.toString();//columnQuote + str + columnQuote;
    }

    public String printTableAlias() {
        String tableName = getFrom().getAlias();

        if (tableName != null) {
            return quoteCol(tableName);
        }

        return "";
    }

    public String printCol(String columnName) {
        if (!columnName.contains(".")) {
            return printTableAlias() + "." + quoteCol(columnName);
        }

        return quoteCol(columnName);
    }

    protected String asVariableName(int valuesPairIdx) {
        return "?";
    }

    protected String asString(String string) {
        return stringQuote + string + stringQuote;
    }

    protected String asString(Term term) {
        String token  = term.token;
        Term   parent = term.getParent();
        if (parent != null && parent.hasToken("eq", "ne", "w", "sw", "ew", "like", "wo")) {
            if (parent.hasToken("w") || parent.hasToken("wo")) {
                if (!token.startsWith("*"))
                    token = "*" + token;

                if (!token.endsWith("*"))
                    token += "*";
            } else if (parent.hasToken("sw") && !token.endsWith("*")) {
                token = token + "*";
            } else if (parent.hasToken("ew") && !token.startsWith("*")) {
                token = "*" + token;
            }

            boolean wildcard = token.indexOf('*') >= 0;
            if (wildcard) {
                token = token.replace("%", "\\%");
                token = token.replace("_", "\\_");
                token = token.replace('*', '%');
            }
        }

        return stringQuote + token + stringQuote;
    }

    protected String asNum(String token) {
        if ("true".equalsIgnoreCase(token))
            return "1";

        if ("false".equalsIgnoreCase(token))
            return "0";

        return token;
    }

    protected boolean isNum(Term term) {
        if (!term.isLeaf() || term.isQuoted())
            return false;

        String token = term.getToken();
        try {
            Double.parseDouble(token);
            return true;
        } catch (Exception ex) {
            //not a number, ignore
        }

        if ("true".equalsIgnoreCase(token))
            return true;

        if ("false".equalsIgnoreCase(token))
            return true;

        return "null".equalsIgnoreCase(token);
    }

    protected boolean isBool(Term term) {
        if (!term.isLeaf() || term.isQuoted())
            return false;

        String token = term.getToken();

        if ("true".equalsIgnoreCase(token))
            return true;

        return "false".equalsIgnoreCase(token);
    }

    public String asBool(String token) {
        if ("true".equalsIgnoreCase(token) || "1".equals(token))
            return "true";

        return "false";
    }

    @Override
    public SqlQuery withDb(D db) {
        super.withDb(db);
        if (db.isType("mysql"))
            withColumnQuote('`');

        return this;
    }

    public SqlQuery withType(String type) {
        this.type = type;
        return this;
    }

    public String getType() {
        if (db != null)
            return db.getType();

        return type;
    }

    public void withStringQuote(char stringQuote) {
        this.stringQuote = stringQuote;
    }

    public void withColumnQuote(char columnQuote) {
        this.columnQuote = columnQuote;
    }

    public static class Parts {

        public String select = null;
        public String from   = null;
        public String where  = null;
        public String group  = null;
        public String order  = null;
        public String limit  = null;

        public List<Term> orderByTerms = new ArrayList();

        void withSql(String sql) {
            if (sql == null)
                return;

            sql = sql.trim();

            SqlTokenizer tok = new SqlTokenizer(sql);

            String clause;
            while ((clause = tok.nextClause()) != null) {
                String token = (clause.indexOf(" ") > 0 ? clause.substring(0, clause.indexOf(" ")) : clause).toLowerCase();

                switch (token) {
                    //case "insert":
                    //case "update":
                    case "delete":
                    case "select":
                        select = clause;
                        continue;
                    case "from":
                        from = clause;
                        continue;
                    case "where":
                        where = clause;
                        continue;
                    case "group":
                        group = clause;
                        continue;
                    case "order":
                        order = clause;
                        continue;
                    case "limit":
                        limit = clause;
                        continue;
                    default:
                        throw new RuntimeException("Unable to parse: \"" + clause + "\" - \"" + sql + "\"");
                }
            }
        }
    }

}

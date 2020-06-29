package io.inversion.elasticsearch;

import io.inversion.rql.Term;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * At the time of this writing, Elastic NestedQueryBuilders do not give access to the nested path
 * once created.  This makes it impossible to create a multi-nested query from the inside out, which
 * is how Inversion RQL is translated.  Therefore, this class was created to maintain access to
 * data used to create the QueryBuilder
 *
 * @author kfrankic
 */
public class WrappedQueryBuilder {
    private QueryBuilder builder;
    private Term         term;
    private String       nestedPath;

    public WrappedQueryBuilder(QueryBuilder qb, Term t, String path) {
        builder = qb;
        term = t;
        nestedPath = path;
    }

    public QueryBuilder getBuilder() {
        return builder;
    }

    public void setBuilder(QueryBuilder builder) {
        this.builder = builder;
    }

    public String getNestedPath() {
        return nestedPath;
    }

    public boolean hasNestedPath() {
        if (nestedPath != null)
            return true;

        return false;
    }

    public String toString() {
        return builder.toString();
    }

}

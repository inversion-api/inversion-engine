package io.inversion;

import io.inversion.rql.Term;
import io.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BooksDb extends Db<BooksDb>{

    public static List<Collection> makeTestCollections(){
        Collection books = new Collection("books")
                .withProperty("bookId", "int", false)//
                .withProperty("authorId", "int")//
                .withProperty("isbn", "string")//
                .withProperty("title", "string")//
                .withIndex("primaryIndex", "primary", true, "bookId");

        Collection authors = new Collection("authors")
                .withProperty("authorId", "int")//
                .withProperty("name", "string")//
                .withIndex("primaryIndex", "primary", true, "authorId");


        Property authors_authorId = authors.getProperty("authorId");
        Property books_authorId = books.getProperty("authorId");
        books_authorId.withPk(authors_authorId);

        Index bookToAuthorForeignKey = new Index("bookToAuthorForeignKey", Index.TYPE_FOREIGN_KEY, false, books_authorId);
        books.withIndexes(bookToAuthorForeignKey);

        return Utils.add(new ArrayList<>(), books, authors);
    }


    protected void buildCollections(){
        makeTestCollections().forEach(c -> withCollections(c));
    }


    @Override
    public Results doSelect(Collection collection, List<Term> queryTerms) throws ApiException {
        return null;
    }

    @Override
    public List<String> doUpsert(Collection collection, List<Map<String, Object>> records) throws ApiException {
        return null;
    }

    @Override
    public void doDelete(Collection collection, List<Map<String, Object>> indexValues) throws ApiException {

    }
}

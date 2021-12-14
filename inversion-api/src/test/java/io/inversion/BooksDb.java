package io.inversion;

import io.inversion.rql.Term;

import java.util.List;
import java.util.Map;

public class BooksDb extends Db<BooksDb>{
    protected void buildCollections(){
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

        withCollections(books, authors);
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

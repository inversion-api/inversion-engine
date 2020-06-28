package io.inversion.elasticsearch;

import io.inversion.rql.Select;
import io.inversion.rql.Term;

/**
 * @author kfrankic
 *
 */
public class ElasticsearchSelect<T extends ElasticsearchSelect, P extends ElasticsearchQuery> extends Select<T, P> {

   public ElasticsearchSelect(P query) {
      super(query);
      clearFunctions();
      withFunctions("source", "includes", "excludes");
   }

   @Override
   protected boolean addTerm(String token, Term term) {
      if (functions.contains(token)) {
         terms.add(term);
         return true;
      }

      return false;
   }

}

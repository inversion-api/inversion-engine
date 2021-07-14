package io.inversion;

import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.curiousoddman.rgxgen.iterators.StringIterator;
import io.inversion.utils.Path;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApiTest {

    @Test
    public void buildOperations_error_on_conflicting_path_params(){
        Api api = new Api().withIncludeOn("*", "test/{var1}/*").withEndpoint("*", "ep1/{var1}/*", new Action()).withCollection(new Collection().withName("test"));

        try{
            api.getOperations();
            fail("Should have thrown an error because variable var1 is mismatched");
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }

    @Test
    public void buildOperations_duplicateOperationIdsGetResolved(){
        Api api = new Api().withIncludeOn("*", "test/{var1}/*").withEndpoint("*", "ep1/{var1}/*", new Action()).withCollection(new Collection().withName("test"));

        try{
            api.getOperations();
            fail("Should have thrown an error because variable var1 is mismatched");
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }


    @Test
    public void temp(){
        //String regex = "(?=[0-9]*)(?=[+-]?([0-9]*[.])?[0-9]+)";
        String regex = "(?=[a-z0-9]+)";
        RgxGen        rgxGen        = new RgxGen(regex);
        StringIterator uniqueStrings = rgxGen.iterateUnique();
        String         match         = uniqueStrings.next();
        if (match.length() == 0)
            match = uniqueStrings.next();
        System.out.println(match);
    }


}

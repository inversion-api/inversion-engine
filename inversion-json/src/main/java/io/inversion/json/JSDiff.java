package io.inversion.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import io.inversion.utils.Utils;

public interface JSDiff {

    JSNode getJson();

    default JSList diff(JSNode source) {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode patch;
        try {
            patch = JsonDiff.asJson(mapper.readValue(source.toString(), JsonNode.class), mapper.readValue(getJson().toString(), JsonNode.class));
            JSList patchesArray = JSParser.asJSList(patch.toPrettyString());
            return patchesArray;
        } catch (Exception e) {
            e.printStackTrace();
            Utils.rethrow(e);
        }

        return null;
    }

}

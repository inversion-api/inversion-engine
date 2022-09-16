package io.inversion.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonPatch;
import io.inversion.utils.Utils;

public interface JSPatch {

    JSNode getJson();

    default void patch(JSList patches) {
        //-- migrate legacy "." based paths to JSONPointer
        for (JSNode patch : patches.asMapList()) {
            Object pathVal = patch.get("path");
            String path = pathVal != null ? pathVal.toString() : null;
            if (path != null && !path.startsWith("/")) {
                path = "/" + path.replace(".", "/");
            }
            patch.put("path", path);

            Object fromVal = patch.get("from");
            path = fromVal != null ? fromVal.toString() : null;
            if (path != null && !path.startsWith("/")) {
                path = "/" + path.replace(".", "/");
                patch.put("from", path);
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode target  = JsonPatch.apply(mapper.readValue(patches.toString(), JsonNode.class), mapper.readValue(getJson().toString(), JsonNode.class));
            JSNode   patched = JSParser.asJSNode(target.toString());
            getJson().clear();
            patched.getProperties().forEach(p -> getJson().put(p.getKey(), p.getValue()));

            if (getJson().isList()) {
                JSList arr = (JSList)getJson();
                arr.clear();
                arr.addAll(((JSList) patched));
            }
        } catch (Exception e) {
            Utils.rethrow(e);
        }
    }

}

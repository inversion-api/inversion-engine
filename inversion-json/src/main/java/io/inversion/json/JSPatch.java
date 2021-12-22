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
            Object pathVal = patch.getValue("path");
            String path = pathVal != null ? pathVal.toString() : null;
            if (path != null && !path.startsWith("/")) {
                path = "/" + path.replace(".", "/");
            }
            patch.putValue("path", path);

            Object fromVal = patch.getValue("from");
            path = fromVal != null ? fromVal.toString() : null;
            if (path != null && !path.startsWith("/")) {
                path = "/" + path.replace(".", "/");
                patch.putValue("from", path);
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode target  = JsonPatch.apply(mapper.readValue(patches.toString(), JsonNode.class), mapper.readValue(getJson().toString(), JsonNode.class));
            JSNode   patched = JSReader.asJSNode(target.toString());
            getJson().clear();
            patched.getProperties().forEach(p -> getJson().putValue(p.getKey(), p.getValue()));

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

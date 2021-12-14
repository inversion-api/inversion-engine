package io.inversion.config;

import io.inversion.*;

public class InversionNamer implements Namer {
    public String name(Context context, Object object) {
        try {
            String name = null;

            if (object instanceof io.inversion.Endpoint) {
                Endpoint ep = (Endpoint) object;
                if (ep.getApi() != null && ep.getName() != null)
                    name = context.makeName(ep.getApi()) + "_endpoints_" + ep.getName();
            } else if (object instanceof io.inversion.Collection) {
                io.inversion.Collection coll = (io.inversion.Collection) object;
                if (coll.getDb() != null && coll.getName() != null)
                    name = context.makeName(coll.getDb()) + "_collections_" + coll.getName();
            } else if (object instanceof Property) {
                Property prop = (Property) object;
                if (prop.getCollection() != null && prop.getName() != null)
                    name = context.makeName(prop.getCollection()) + "_properties_" + prop.getName();
            } else if (object instanceof Index) {
                Index index = (Index) object;
                if (index.getCollection() != null && index.getName() != null)
                    name = context.makeName(index.getCollection()) + "_indexes_" + index.getName();
            } else if (object instanceof Relationship) {
                Relationship rel = (Relationship) object;
                if (rel.getCollection() != null && rel.getName() != null)
                    name = context.makeName(rel.getCollection()) + "_relationships_" + rel.getName();
            }

            return name;

        } catch (Exception ex) {
            throw new ApiException(ex);
        }
    }
}

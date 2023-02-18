package io.inversion.context;

import io.inversion.*;

public class InversionNamer implements Namer {
    public String name(Context context, Object object) {
        try {
            String name = null;

            if (object instanceof io.inversion.Endpoint) {
                Endpoint ep = (Endpoint) object;
                if (ep.getApi() != null && ep.getName() != null)
                    name = makeName(context, ep.getApi(), "_endpoints_", ep.getName());
            } else if (object instanceof io.inversion.Collection) {
                io.inversion.Collection coll = (io.inversion.Collection) object;
                if (coll.getDb() != null && coll.getName() != null)
                    name = makeName(context, coll.getDb(), "_collections_", coll.getName());
            } else if (object instanceof Property) {
                Property prop = (Property) object;
                if (prop.getCollection() != null && prop.getName() != null)
                    name = makeName(context, prop.getCollection(), "_properties_", prop.getName());
            } else if (object instanceof Index) {
                Index index = (Index) object;
                if (index.getCollection() != null && index.getName() != null)
                    name = makeName(context, index.getCollection(), "_indexes_", index.getName());
            } else if (object instanceof Relationship) {
                Relationship rel = (Relationship) object;
                if (rel.getCollection() != null && rel.getName() != null)
                    name = makeName(context, rel.getCollection(), "_relationships_", rel.getName());
            }

            if(name != null)
                name = name.replaceAll("[^A-Za-z0-9]", "_");

            return name;

        } catch (Exception ex) {
            throw new ApiException(ex);
        }
    }

    public String makeName(Context context, Object parent, String prefix, String name, String... choices) {
        String parentName = context.makeName(parent);
        if (name.startsWith(parentName + prefix))
            return name;
        return parentName + prefix + name;
    }
}

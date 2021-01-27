package io.inversion.action.security.schemes;

import io.inversion.ApiException;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.User;
import io.inversion.action.security.AuthScheme;

public class BasicScheme  extends AuthScheme {

    BearerScheme.RevokedTokenCache revokedTokenCache = null;

    public BasicScheme() {
        withName("basicAuth");
        withType("http");
        withScheme("basic");
    }

    @Override
    public User getUser(Request req, Response res) throws ApiException {
        return null;
    }
}

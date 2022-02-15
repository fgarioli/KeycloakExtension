/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.fgarioli.keycloak;

import java.util.stream.Stream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

/**
 *
 * @author fernando
 */
public class GroupExtensionProvider implements RealmResourceProvider {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final GroupModel group;

    public GroupExtensionProvider(RealmModel realm, GroupModel group, AdminPermissionEvaluator auth, KeycloakSession session) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.group = group;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }

    /**
     * Get users
     *
     * Returns a stream of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param briefRepresentation Only return basic information (only guaranteed
     * to return id, username, created, first and last name, email, enabled
     * state, email verification state, federation link, and access. Note that
     * it means that namely user attributes, required actions, and not before
     * are not returned.)
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Path("subgroupmembers")
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserRepresentation> getMembers(@QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults,
            @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @QueryParam("subgroups") Boolean subgroups) {
        this.auth.groups().requireViewMembers(group);

        firstResult = firstResult != null ? firstResult : 0;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;

        return session.users().getGroupMembersStream(realm, group, firstResult, maxResults)
                .map(user -> briefRepresentationB
                ? ModelToRepresentation.toBriefRepresentation(user)
                : ModelToRepresentation.toRepresentation(session, realm, user));
    }

}

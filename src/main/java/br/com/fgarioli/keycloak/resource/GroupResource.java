/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.fgarioli.keycloak.resource;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

/**
 *
 * @author fernando
 */
public class GroupResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;

    public GroupResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
    }

    /**
     * Get users
     *
     * Returns a stream of users, filtered according to query parameters
     *
     * @param id
     * @param briefRepresentation Only return basic information (only guaranteed
     * to return id, username, created, first and last name, email, enabled
     * state, email verification state, federation link, and access. Note that
     * it means that namely user attributes, required actions, and not before
     * are not returned.)
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Path("/groups/{id}/members/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserRepresentation> getMembersSubgroups(
            @PathParam("id") String id,
            @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        GroupModel group = realm.getGroupById(id);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;

        List<UserRepresentation> members = this.getSubgroupMembers(group, briefRepresentationB);

        HashSet<Object> seen = new HashSet<>();
        members.removeIf(e -> !seen.add(e.getId()));

        return members.stream();
    }

    private List<UserRepresentation> getSubgroupMembers(GroupModel group, Boolean briefRepresentation) {
        this.auth.groups().requireViewMembers(group);

        List<UserRepresentation> users = session.users().getGroupMembersStream(realm, group)
                .map(user -> briefRepresentation
                ? ModelToRepresentation.toBriefRepresentation(user)
                : ModelToRepresentation.toRepresentation(session, realm, user))
                .collect(Collectors.toList());

        group.getSubGroupsStream().forEach(subgroup -> {
            users.addAll(this.getSubgroupMembers(subgroup, briefRepresentation));
        });

        return users;
    }

}

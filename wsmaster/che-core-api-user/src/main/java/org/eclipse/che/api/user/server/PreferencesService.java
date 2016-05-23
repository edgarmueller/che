package org.eclipse.che.api.user.server;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_PREFERENCES;

/**
 * Preferences REST API.
 *
 * @author Yevhenii Voevodin
 */
@Path("/preferences")
public class PreferencesService extends Service {

    @Inject
    private PreferencesManager preferencesManager;

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    @ApiOperation(value = "Get user preferences",
                  notes = "Get user preferences, like SSH keys, recently opened project and files. It is possible " +
                          "to use a filter, e.g. CodenvyAppState or ssh.key.public.github.com to get the last opened project " +
                          "or a public part of GitHub SSH key (if any)")
    @ApiResponses({@ApiResponse(code = 200, message = "Preferences successfully fetched"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public Map<String, String> find(@ApiParam("Filer") @QueryParam("filter") String filter) throws ServerException {
        if (filter == null) {
            return preferencesManager.find(subjectId());
        }
        return preferencesManager.find(subjectId(), filter);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed("user")
    public void save(Map<String, String> preferences) throws BadRequestException, ServerException {
        if (preferences == null) {
            throw new BadRequestException("Required non-null new preferences");
        }
        preferencesManager.save(subjectId(), preferences);
    }

    @PUT
    @RolesAllowed("user")
    @GenerateLink(rel = LINK_REL_PREFERENCES)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Map<String, String> update(Map<String, String> preferences) throws ServerException, BadRequestException {
        if (preferences == null) {
            throw new BadRequestException("Required non-null preferences update");
        }
        return preferencesManager.update(subjectId(), preferences);
    }

    @DELETE
    @RolesAllowed("user")
    @Consumes(APPLICATION_JSON)
    @GenerateLink(rel = LINK_REL_PREFERENCES)
    @ApiOperation(value = "Remove preferences of current user.",
                  notes = "if names are not specified, then all the user's preferences will be removed, " +
                          "otherwise only the preferences which names are listed")
    @ApiResponses({@ApiResponse(code = 204, message = "OK"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 409, message = "Preferences names required"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public void removePreferences(@ApiParam("Preferences to remove") List<String> names) throws ServerException {
        if (names == null || names.isEmpty()) {
            preferencesManager.remove(subjectId());
        } else {
            preferencesManager.remove(subjectId(), names);
        }
    }

    private static String subjectId() {
        return EnvironmentContext.getCurrent().getUser().getId();
    }
}

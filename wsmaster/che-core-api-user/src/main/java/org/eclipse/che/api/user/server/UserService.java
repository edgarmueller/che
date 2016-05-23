/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.user.server;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.model.user.User;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.user.shared.dto.UserDto;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_CURRENT_USER;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_USER_BY_EMAIL;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_USER_BY_ID;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_REMOVE_USER_BY_ID;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_UPDATE_PASSWORD;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_USER;
import static org.eclipse.che.api.user.server.DtoConverter.asDto;

/**
 * User REST API.
 *
 * @author Yevhenii Voevodin
 * @author Anton Korneta
 */
@Path("/user")
@Api(value = "/user", description = "User REST API")
public class UserService extends Service {
    @VisibleForTesting
    static final String USER_SELF_CREATION_ALLOWED = "user.self.creation.allowed";

    private final UserManager       userManager;
    private final TokenValidator    tokenValidator;
    private final UserLinksInjector linksInjector;
    private final boolean           userSelfCreationAllowed;

    @Context
    private SecurityContext context;

    @Inject
    public UserService(UserManager userManager,
                       TokenValidator tokenValidator,
                       UserLinksInjector linksInjector,
                       @Named(USER_SELF_CREATION_ALLOWED) boolean userSelfCreationAllowed) {
        this.userManager = userManager;
        this.linksInjector = linksInjector;
        this.tokenValidator = tokenValidator;
        this.userSelfCreationAllowed = userSelfCreationAllowed;
    }

    @POST
    @Path("/user")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @GenerateLink(rel = LINK_REL_USER)
    @ApiOperation(value = "Creates a new user", response = UserDto.class)
    @ApiResponses({@ApiResponse(code = 201, message = "Created"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 401, message = "Missed token parameter"),
                   @ApiResponse(code = 403, message = "Invalid or missing request parameters"),
                   @ApiResponse(code = 409, message = "Invalid token"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public Response create(@ApiParam(value = "New user")
                           UserDto userDto,
                           @ApiParam(value = "Authentication token")
                           @QueryParam("token")
                           String token,
                           @ApiParam(value = "User type")
                           @QueryParam("temporary")
                           @DefaultValue("false")
                           Boolean isTemporary,
                           @Context
                           SecurityContext context) throws ForbiddenException,
                                                           BadRequestException,
                                                           UnauthorizedException,
                                                           ConflictException,
                                                           ServerException,
                                                           NotFoundException {
        final User user = context.isUserInRole("system/admin") ? fromEntity(userDto) : fromToken(token);
        userManager.create(user, isTemporary);
        return Response.status(CREATED)
                       .entity(linksInjector.injectLinks(asDto(user), getServiceContext()))
                       .build();
    }

    @GET
    @GenerateLink(rel = LINK_REL_GET_CURRENT_USER)
    @RolesAllowed({"user", "temp_user"})
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get current user",
                  notes = "Get user currently logged in the system",
                  response = UserDto.class,
                  position = 2)
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public UserDto getCurrent() throws NotFoundException, ServerException {
        final User user = userManager.getById(subjectId());
        return linksInjector.injectLinks(asDto(user), getServiceContext());
    }

    @POST
    @Path("/password")
    @GenerateLink(rel = LINK_REL_UPDATE_PASSWORD)
    @RolesAllowed("user")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @ApiOperation(value = "Update password",
                  notes = "Update current password")
    @ApiResponses({@ApiResponse(code = 204, message = "OK"),
                   @ApiResponse(code = 400, message = "Invalid password"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public void updatePassword(@ApiParam(value = "New password", required = true)
                               @FormParam("password")
                               String password) throws NotFoundException,
                                                       BadRequestException,
                                                       ServerException,
                                                       ConflictException {
        checkPassword(password);

        final UserImpl user = userManager.getById(subjectId());
        user.setPassword(password);
        userManager.update(user);
    }

    @GET
    @Path("/{id}")
    @GenerateLink(rel = LINK_REL_GET_USER_BY_ID)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get user by ID",
                  notes = "Get user by its ID in the system. Roles allowed: system/admin, system/manager.",
                  response = UserDto.class)
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public UserDto getById(@ApiParam(value = "User ID") @PathParam("id") String id) throws NotFoundException,
                                                                                           ServerException {
        final User user = userManager.getById(id);
        return linksInjector.injectLinks(asDto(user), getServiceContext());
    }

    @GET
    @Path("/find")
    @GenerateLink(rel = LINK_REL_GET_USER_BY_EMAIL)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get user by alias",
                  notes = "Get user by alias. Roles allowed: system/admin, system/manager.",
                  response = UserDto.class)
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 400, message = "Missed alias parameter"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public UserDto getByAlias(@ApiParam(value = "User alias", required = true)
                              @QueryParam("alias")
                              @Required String alias) throws NotFoundException,
                                                             ServerException,
                                                             BadRequestException {
        if (alias == null) {
            throw new BadRequestException("Missed parameter alias");
        }
        final User user = userManager.getByAlias(alias);
        return linksInjector.injectLinks(asDto(user), getServiceContext());
    }

    @DELETE
    @Path("/{id}")
    @GenerateLink(rel = LINK_REL_REMOVE_USER_BY_ID)
    @RolesAllowed("system/admin")
    @ApiOperation(value = "Delete user",
                  notes = "Delete a user from the system. Roles allowed: system/admin")
    @ApiResponses({@ApiResponse(code = 204, message = "Deleted"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 409, message = "Impossible to remove user"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public void remove(@ApiParam(value = "User ID") @PathParam("id") String id) throws NotFoundException,
                                                                                       ServerException,
                                                                                       ConflictException {
        userManager.remove(id);
    }

    @GET
    @Path("/name/{name}")
    @GenerateLink(rel = "get user by name")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get user by name",
                  notes = "Get user by its name in the system. Roles allowed: user, system/admin, system/manager.")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    public UserDto getByName(@ApiParam(value = "User email")
                             @PathParam("name")
                             String name) throws NotFoundException, ServerException {
        final User user = userManager.getByName(name);
        return linksInjector.injectLinks(asDto(user), getServiceContext());
    }

    @GET
    @Path("/settings")
    @Produces(APPLICATION_JSON)
    public Map<String, String> getSettings() {
        return ImmutableMap.of(USER_SELF_CREATION_ALLOWED, Boolean.toString(userSelfCreationAllowed));
    }

    private User fromEntity(UserDto userDto) throws BadRequestException {
        checkUser(userDto);
        return new UserImpl(null,
                            userDto.getEmail(),
                            userDto.getName(),
                            userDto.getPassword(),
                            null);
    }

    private User fromToken(String token) throws UnauthorizedException, ConflictException {
        if (token == null) {
            throw new UnauthorizedException("Missed token parameter");
        }
        final String email = tokenValidator.validateToken(token);
        final int atIdx = email.indexOf('@');
        // Getting all the characters before '@' e.g. user@codenvy.com -> user
        final String name = atIdx == -1 ? email : email.substring(0, atIdx);
        return new UserImpl(null, email, name, null, null);
    }

    private static void checkUser(User user) throws BadRequestException {
        if (user == null) {
            throw new BadRequestException("User Descriptor required");
        }
        if (isNullOrEmpty(user.getName())) {
            throw new BadRequestException("User name required");
        }
        if (isNullOrEmpty(user.getEmail())) {
            throw new BadRequestException("User email required");
        }
        if (user.getPassword() != null) {
            checkPassword(user.getPassword());
        }
    }

    private static void checkPassword(String password) throws BadRequestException {
        if (password == null) {
            throw new BadRequestException("Password required");
        }
        if (password.length() < 8) {
            throw new BadRequestException("Password should contain at least 8 characters");
        }
        int numOfLetters = 0;
        int numOfDigits = 0;
        for (char passwordChar : password.toCharArray()) {
            if (Character.isDigit(passwordChar)) {
                numOfDigits++;
            } else if (Character.isLetter(passwordChar)) {
                numOfLetters++;
            }
        }
        if (numOfDigits == 0 || numOfLetters == 0) {
            throw new BadRequestException("Password should contain letters and digits");
        }
    }

    private static String subjectId() {
        return EnvironmentContext.getCurrent().getUser().getId();
    }
}

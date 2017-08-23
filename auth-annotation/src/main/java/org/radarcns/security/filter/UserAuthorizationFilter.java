package org.radarcns.security.filter;

import org.radarcns.security.annotation.Secured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dverbeec on 17/03/2017.
 */
@Secured
@Provider
@Priority(Priorities.AUTHORIZATION)
/**
 * Authorization filter for RADAR. This class checks if the authenticated user is actually
 * authorized to perform the requested action. It is assumed the security context is already
 * populated with the correct user name and user roles.
 */
public class UserAuthorizationFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserAuthorizationFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    public static final String PROJECT_PATH_PARAM = "studyID";
    public static final String USERID_PATH_PARAM = "userID";

    @Override
    public void filter(ContainerRequestContext requestContext) {

        SecurityContext securityContext = requestContext.getSecurityContext();

        MultivaluedMap<String, String> pathParameters =
            requestContext.getUriInfo().getPathParameters();

        // Get the resource method which matches with the requested URL
        // Extract the scopes declared by it
        Method resourceMethod = resourceInfo.getResourceMethod();
        List<String> allowedRoles = extractRoles(resourceMethod);

        if (allowedRoles.isEmpty()) {
            log.debug("Method is secured but no roles defined, using roles "
                        + "defined on class level to check authorization");
            // Get the resource class which matches with the requested URL
            // Extract the scopes declared by it
            Class<?> resourceClass = resourceInfo.getResourceClass();
            allowedRoles = extractRoles(resourceClass);
        }

        /* disregard user roles until implementation of project/collection is ready
        try {
            isUserAuthorized(securityContext, allowedRoles, pathParameters);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }*/
        log.debug("User is authorized for this resource");
    }

    // Extract the scopes from the annotated element
    public List<String> extractRoles(AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            return new ArrayList<>();
        } else {
            Secured secured = annotatedElement.getAnnotation(Secured.class);
            if (secured == null) {
                return new ArrayList<>();
            } else {
                String[] rolesAllowed = secured.rolesAllowed();
                if (rolesAllowed != null) {
                    return Arrays.asList(rolesAllowed);
                }
                else {
                    return new ArrayList<>();
                }
            }
        }
    }


    private void checkUserRoles(List<String> rolesAllowed, String projectID, SecurityContext
            securityContext) throws NotAuthorizedException {
        log.debug("Checking user roles");
        if (rolesAllowed.isEmpty()) {
            log.debug("No allowed roles defined, assuming any role is authorized "
                        + "for this resource");
            return;
        }
        for (String role : rolesAllowed) {
            if (securityContext.isUserInRole(projectID + ":" + role)) {
                log.debug("User role " + role + " matched allowed role");
                return;
            }
        }
        throw new NotAuthorizedException("User does not have the appropriate role for this method",
            Response.status(Response.Status.FORBIDDEN));
    }

    /**
     * Checks if a user is authorized, based on the current security context, the allowed roles
     * for the requested operation and the path parameters. Check here for the flowchart:
     * https://docs.google.com/drawings/d/13G7INHdfTZSXLqKETzsudaaIHrLcU4V9Ab_kwkYZXTs/edit?ths=true
     * @param securityContext The current JAX-RS security context
     * @param rolesAllowed The roles that are allowed for this resource
     * @param pathParameters The path parameters supplied in the request. In practice, only in
     *                       certain conditions this is checked. See the flowchart for more details.
     * @throws NotAuthorizedException When the user is not authorized to access the resource
     */
    public void isUserAuthorized(SecurityContext securityContext, List<String> rolesAllowed,
                MultivaluedMap<String, String> pathParameters) throws NotAuthorizedException {
        String authorizedUserId = securityContext.getUserPrincipal().getName();
        String projectID = pathParameters.getFirst(PROJECT_PATH_PARAM);
        String userID = pathParameters.getFirst(USERID_PATH_PARAM);

        if (projectID == null) {
            throw new NotAuthorizedException(PROJECT_PATH_PARAM + " path parameter must be "
                + "supplied.", Response.status(Response.Status.FORBIDDEN));
        }

        if (userID != null && userID.equals(authorizedUserId)) {
            // user requested own data, this is allowed
            return;
        }

        checkUserRoles(rolesAllowed, projectID, securityContext);
    }
}

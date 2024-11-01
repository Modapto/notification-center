package gr.atc.modapto.util;

import gr.atc.modapto.enums.UserRole;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import static gr.atc.modapto.exception.CustomExceptions.*;

/*
 * Utility class to parse the JWT received token and extract user roles
 */
public class JwtUtils {
    private static final String ID_FIELD = "sub";
    private static final String PILOT_ROLE_FIELD = "pilot_role";
    private static final String JWT_ERROR = "Invalid JWT token inserted";

    private JwtUtils() {}

    /**
     * Util to retrieve pilot Code from JWT Token
     * @param jwt Token to extract userId
     * @return userId
     */
    public static String extractUserId(Jwt jwt){
        if (jwt == null || jwt.getClaimAsString(ID_FIELD) == null) {
            throw new JwtTokenException(JWT_ERROR);
        }
        return jwt.getClaimAsString(ID_FIELD);
    }

    /**
     * Util to retrieve user role of user from JWT Token
     * @param jwt Token to extract user role
     * @return User Role
     */
    public static UserRole extractPilotRole(Jwt jwt){
        if (jwt == null || jwt.getClaimAsString(PILOT_ROLE_FIELD) == null) {
            throw new JwtTokenException(JWT_ERROR);
        }
        if (EnumUtils.isValidEnumIgnoreCase(UserRole.class, jwt.getClaimAsString(PILOT_ROLE_FIELD)))
            return UserRole.valueOf(jwt.getClaimAsString(PILOT_ROLE_FIELD));

        return null;
    }
}

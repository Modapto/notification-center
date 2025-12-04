package gr.atc.modapto.exception;

public class CustomExceptions {

    private CustomExceptions(){}

    /*
     * Exception thrown when requested data is not found in an index in DB
     */
    public static class DataNotFoundException extends RuntimeException {
        public DataNotFoundException(String message) {
            super(message);
        }
    }

    /*
     * Exception thrown when an exception occurs in the mapping process between DTO and Models
     */
    public static class ModelMappingException extends RuntimeException{
        public ModelMappingException(String message){
            super(message);
        }
    }

    /*
     * Exception thrown when a JWT token is invalid
     */
    public static class JwtTokenException extends RuntimeException{
        public JwtTokenException(String message){
            super(message);
        }
    }

    /*
     * Exception thrown when a user that is not involved in an assignment tries to update it
     */
    public static class UnauthorizedAssignmentUpdateException extends RuntimeException{
        public UnauthorizedAssignmentUpdateException(String message){
            super(message);
        }
    }
}

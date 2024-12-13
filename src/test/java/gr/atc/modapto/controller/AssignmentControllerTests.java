package gr.atc.modapto.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.modapto.dto.AssignmentCommentDto;
import gr.atc.modapto.dto.AssignmentDto;
import gr.atc.modapto.enums.AssignmentStatus;
import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.exception.CustomExceptions;
import gr.atc.modapto.service.IAssignmentService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AssignmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IAssignmentService assignmentService;

    private AssignmentDto testAssignment;
    private AssignmentCommentDto testComment;
    private Page<AssignmentDto> paginatedResults;
    private Jwt jwt;

    @BeforeEach
    void setup() {
        testAssignment = AssignmentDto.builder()
                .id("123")
                .priority(MessagePriority.HIGH)
                .sourceUserId("sourceUser")
                .targetUserId("targetUser")
                .sourceUserComments(new HashMap<>())
                .targetUserComments(new HashMap<>())
                .timestamp(LocalDateTime.now())
                .status(AssignmentStatus.OPEN)
                .timestampUpdated(LocalDateTime.now())
                .description("Test")
                .build();

        testComment = new AssignmentCommentDto();
        testComment.setSourceUserComment("Test Source Comment");
        testComment.setTargetUserComment("Test Target Comment");

        paginatedResults = new PageImpl<>(List.of(testAssignment), PageRequest.of(0, 10), 1);

        Map<String, Object> claims = new HashMap<>();
        claims.put("realm_access", Map.of("roles", List.of("SUPER_ADMIN")));
        claims.put("resource_access", Map.of("modapto", Map.of("roles", List.of("SUPER_ADMIN"))));
        claims.put("sub", "user");
        claims.put("pilot", "TEST");
        claims.put("pilot_role", "TEST");

        String tokenValue = "mock.jwt.token";
        jwt = Jwt.withTokenValue(tokenValue)
                .headers(header -> header.put("alg", "HS256"))
                .claims(claim -> claim.putAll(claims))
                .build();
    }

    @DisplayName("Get All Assignments: Success")
    @Test
    void givenValidRequest_whenGetAllAssignments_thenReturnAssignmentsList() throws Exception {
        given(assignmentService.retrieveAllAssignments(any())).willReturn(paginatedResults);

        mockMvc.perform(get("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignments retrieved successfully!")))
                .andExpect(jsonPath("$.data.totalPages", is(1)))
                .andExpect(jsonPath("$.data.lastPage", is(true)))
                .andExpect(jsonPath("$.data.results[0].assignmentId", is("123")));
    }

    @DisplayName("Get All Assignments: Invalid Sort Attribute")
    @Test
    void givenInvalidSortAttribute_whenGetAllAssignments_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/assignments?sortAttribute=invalidAttribute")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid sort attribute")));
    }

    @DisplayName("Get Assignments per User (No filters): Success")
    @Test
    void givenValidUserId_whenGetAllAssignmentPerUser_thenReturnAssignmentsList() throws Exception {
        given(assignmentService.retrieveAssignmentsPerUserId(any(), any(), any()))
                .willReturn(paginatedResults);

        mockMvc.perform(get("/api/assignments/user/123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignments retrieved successfully!")))
                .andExpect(jsonPath("$.data.totalPages", is(1)));
    }

    @DisplayName("Get Assignments per User (With Status Filter): Success")
    @Test
    void givenValidUserIdAndStatus_whenGetAllAssignmentPerUser_thenReturnFilteredAssignments() throws Exception {
        // Given
        given(assignmentService.retrieveAssignmentsPerUserIdAndStatus(any(), any(), eq("OPEN"), any()))
                .willReturn(paginatedResults);

        // When - Then
        mockMvc.perform(get("/api/assignments/user/123")
                        .param("status", "open")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignments retrieved successfully!")))
                .andExpect(jsonPath("$.data.totalPages", is(1)))
                .andExpect(jsonPath("$.data.results[0].assignmentId", is("123")));
    }

    @DisplayName("Get Assignments per User (With Type Filter): Success")
    @Test
    void givenValidUserIdAndType_whenGetAllAssignmentPerUser_thenReturnFilteredAssignments() throws Exception {
        // Mocking the service call for a valid type filter
        given(assignmentService.retrieveAssignmentsPerUserId(any(), eq("REQUESTED"), any()))
                .willReturn(paginatedResults);

        mockMvc.perform(get("/api/assignments/user/123")
                        .param("type", "requested")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignments retrieved successfully!")))
                .andExpect(jsonPath("$.data.totalPages", is(1)))
                .andExpect(jsonPath("$.data.results[0].assignmentId", is("123")));
    }

    @DisplayName("Get Assignments per User (With Status and Type Filter): Success")
    @Test
    void givenValidUserIdStatusAndType_whenGetAllAssignmentPerUser_thenReturnFilteredAssignments() throws Exception {
        // Mocking the service call for both status and type filters
        given(assignmentService.retrieveAssignmentsPerUserIdAndStatus(any(), eq("REQUESTED"), eq("OPEN"), any()))
                .willReturn(paginatedResults);

        mockMvc.perform(get("/api/assignments/user/123")
                        .param("status", "open")
                        .param("type", "requested")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignments retrieved successfully!")))
                .andExpect(jsonPath("$.data.totalPages", is(1)))
                .andExpect(jsonPath("$.data.results[0].assignmentId", is("123")));
    }

    @DisplayName("Get Assignments per User (Invalid Sort Attribute): Bad Request")
    @Test
    void givenInvalidSortAttribute_whenGetAllAssignmentPerUser_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/assignments/user/123")
                        .param("sortAttribute", "invalidAttribute")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid sort attribute")));
    }

    @DisplayName("Get Assignments per User (With Pagination): Success")
    @Test
    void givenValidUserIdAndPagination_whenGetAllAssignmentPerUser_thenReturnPaginatedResults() throws Exception {
        // Mock Pagination
        PageRequest pageable = PageRequest.of(1, 5);
        Page<AssignmentDto> paginatedResults = new PageImpl<>(List.of(testAssignment), pageable, 15);

        // Given
        given(assignmentService.retrieveAssignmentsPerUserId(any(), any(), any()))
                .willReturn(paginatedResults);

        // When - Then
        mockMvc.perform(get("/api/assignments/user/123")
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalPages", is(3)))
                .andExpect(jsonPath("$.data.totalElements", is(15)))
                .andExpect(jsonPath("$.data.results[0].assignmentId", is("123")));
    }

    @DisplayName("Get Assignments per User (Invalid Status Filter): Bad Request")
    @Test
    void givenInvalidStatusFilter_whenGetAllAssignmentPerUser_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/assignments/user/123")
                        .param("status", "INVALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.status", is("Invalid assignment status. Only OPEN, ACCEPTED, IN_PROGRESS and COMPLETED are allowed.")))
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @DisplayName("Get Assignments per User (Invalid Type Filter): Bad Request")
    @Test
    void givenInvalidTypeFilter_whenGetAllAssignmentPerUser_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/assignments/user/123")
                        .param("type", "INVALID_TYPE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.type", is("Invalid assignment type. Only 'Requested' or 'Received' are allowed.")))
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @DisplayName("Get Assignment by ID: Success")
    @Test
    void givenValidAssignmentId_whenGetAssignmentById_thenReturnAssignment() throws Exception {
        given(assignmentService.retrieveAssignmentById("assignmentId")).willReturn(testAssignment);

        mockMvc.perform(get("/api/assignments/assignmentId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentId", is("123")))
                .andExpect(jsonPath("$.message", is("Assignment retrieved successfully")));
    }

    @DisplayName("Get Assignment by ID: Not Found")
    @Test
    void givenInvalidAssignmentId_whenGetAssignmentById_thenReturnNotFound() throws Exception {
        given(assignmentService.retrieveAssignmentById(any())).willThrow(CustomExceptions.DataNotFoundException.class);

        mockMvc.perform(get("/api/assignments/invalidId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested resource not found in DB")));
    }

    @DisplayName("Delete Assignment by ID: Success")
    @Test
    void givenValidAssignmentId_whenDeleteAssignmentById_thenReturnSuccess() throws Exception {
        given(assignmentService.deleteAssignmentById("assignmentId")).willReturn(true);

        mockMvc.perform(delete("/api/assignments/assignmentId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignment deleted successfully!")));
    }

    @DisplayName("Delete Assignment by ID: Not Found message")
    @Test
    void givenInvalidAssignmentId_whenDeleteAssignmentById_thenReturnNotFoundMessage() throws Exception {
        given(assignmentService.deleteAssignmentById(anyString())).willThrow(CustomExceptions.DataNotFoundException.class);

        mockMvc.perform(delete("/api/assignments/invalidId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested resource not found in DB")));
    }

    @DisplayName("Create Assignment: Success")
    @Test
    void givenValidAssignmentDto_whenCreateAssignment_thenReturnAssignmentId() throws Exception {
        // Given
        AssignmentDto newAssignment = new AssignmentDto();
        newAssignment.setDescription("Test");
        newAssignment.setPriority(MessagePriority.HIGH);
        newAssignment.setTargetUserId("456");
        newAssignment.setProductionModule("Test Module");

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.complete(null);

        given(assignmentService.storeAssignment(any(AssignmentDto.class))).willReturn("123");
        given(assignmentService.createNotificationAndNotifyUser(any(AssignmentDto.class))).willReturn(completableFuture);

        // Mock JWT authentication
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);

        // When - Then
        mockMvc.perform(post("/api/assignments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAssignment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignment created successfully")))
                .andExpect(jsonPath("$.data", is("123")));
    }

    @DisplayName("Create Assignment: Failure")
    @Test
    void givenInvalidData_whenCreateAssignment_thenReturnServerError() throws Exception {
        given(assignmentService.storeAssignment(any(AssignmentDto.class))).willReturn(null);

        // Mock JWT authentication
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);

        mockMvc.perform(post("/api/assignments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAssignment)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Unable to create and store assignment")));
    }

    @DisplayName("Create Assignment: Unauthorized")
    @Test
    void givenNoToken_whenCreateAssignment_thenReturnUnauthorized() throws Exception {
        given(assignmentService.storeAssignment(any(AssignmentDto.class))).willReturn(null);

        mockMvc.perform(post("/api/assignments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAssignment)))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Update Assignment: Success")
    @Test
    void givenValidAssignmentIdAndDto_whenUpdateAssignment_thenReturnSuccess() throws Exception {
        // When - Then
        mockMvc.perform(put("/api/assignments/assignmentId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAssignment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignment updated successfully!")));
    }

    @DisplayName("Update Assignment Comments: Success")
    @Test
    void givenValidAssignmentIdAndCommentDto_whenUpdateAssignmentComments_thenReturnSuccess() throws Exception {
        mockMvc.perform(put("/api/assignments/assignmentId/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignment comments updated successfully!")));
    }
}

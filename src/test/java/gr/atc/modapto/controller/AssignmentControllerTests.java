package gr.atc.modapto.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;

import gr.atc.modapto.repository.AssignmentRepository;
import gr.atc.modapto.repository.EventMappingsRepository;
import gr.atc.modapto.repository.EventRepository;
import gr.atc.modapto.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import gr.atc.modapto.service.interfaces.IAssignmentService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(AssignmentController.class)
@ActiveProfiles("test")
class AssignmentControllerTests {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private ElasticsearchTemplate elasticsearchTemplate;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private AssignmentRepository assignmentRepository;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private EventMappingsRepository eventMappingsRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAssignmentService assignmentService;

    private AssignmentDto testAssignment;
    private AssignmentCommentDto testComment;
    private Page<AssignmentDto> paginatedResults;
    private Jwt jwt;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testComment = AssignmentCommentDto.builder()
                .comment("Test comment")
                .build();

        testAssignment = AssignmentDto.builder()
                .id("123")
                .priority(MessagePriority.High.toString())
                .sourceUserId("sourceUser")
                .targetUserId("targetUser")
                .comments(List.of(testComment))
                .timestamp(LocalDateTime.now())
                .status(AssignmentStatus.Open.toString())
                .timestampUpdated(LocalDateTime.now())
                .description("Test")
                .build();

        paginatedResults = new PageImpl<>(List.of(testAssignment), PageRequest.of(0, 10), 1);

        Map<String, Object> claims = new HashMap<>();
        claims.put("realm_access", Map.of("roles", List.of("SUPER_ADMIN")));
        claims.put("resource_access", Map.of("modapto", Map.of("roles", List.of("SUPER_ADMIN"))));
        claims.put("sub", "user");
        claims.put("pilot_code", "SEW");
        claims.put("pilot_role", "TEST");
        claims.put("user_role", "TEST_ROLE");

        String tokenValue = "mock.jwt.token";
        jwt = Jwt.withTokenValue(tokenValue)
                .headers(header -> header.put("alg", "HS256"))
                .claims(claim -> claim.putAll(claims))
                .build();
    }

    @DisplayName("Get All Assignments: Success")
    @WithMockUser
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
    @WithMockUser
    @Test
    void givenInvalidSortAttribute_whenGetAllAssignments_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/assignments?sortAttribute=invalidAttribute")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid sort attribute")));
    }

    @DisplayName("Get Assignments per User (No filters): Success")
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
    @Test
    void givenInvalidStatusFilter_whenGetAllAssignmentPerUser_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/assignments/user/123")
                        .param("status", "INVALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.status", is("Invalid assignment status. Only 'Open', 'Re_Open', 'In_Progress' and 'Done' are allowed.")))
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @DisplayName("Get Assignments per User (Invalid Type Filter): Bad Request")
    @WithMockUser
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
    @WithMockUser
    @Test
    void givenValidAssignmentId_whenGetAssignmentById_thenReturnAssignment() throws Exception {
        given(assignmentService.retrieveAssignmentById("assignmentId")).willReturn(testAssignment);

        mockMvc.perform(get("/api/assignments/assignmentId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentId", is("123")))
                .andExpect(jsonPath("$.message", is("Assignment retrieved successfully")))
                .andExpect(jsonPath("$.data.comments[0].comment", is("Test comment")));
    }

    @DisplayName("Get Assignment by ID: Not Found")
    @WithMockUser
    @Test
    void givenInvalidAssignmentId_whenGetAssignmentById_thenReturnNotFound() throws Exception {
        given(assignmentService.retrieveAssignmentById(any())).willThrow(CustomExceptions.DataNotFoundException.class);

        mockMvc.perform(get("/api/assignments/invalidId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested resource not found in DB")));
    }

    @DisplayName("Delete Assignment by ID: Success")
    @WithMockUser
    @Test
    void givenValidAssignmentId_whenDeleteAssignmentById_thenReturnSuccess() throws Exception {
        given(assignmentService.deleteAssignmentById("assignmentId")).willReturn(true);

        mockMvc.perform(delete("/api/assignments/assignmentId")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignment deleted successfully!")));
    }

    @DisplayName("Delete Assignment by ID: Not Found message")
    @WithMockUser
    @Test
    void givenInvalidAssignmentId_whenDeleteAssignmentById_thenReturnNotFoundMessage() throws Exception {
        given(assignmentService.deleteAssignmentById(anyString())).willThrow(CustomExceptions.DataNotFoundException.class);

        mockMvc.perform(delete("/api/assignments/invalidId")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested resource not found in DB")));
    }

    @DisplayName("Create Assignment: Success")
    @Test
    void givenValidAssignmentDto_whenCreateAssignment_thenReturnAssignmentId() throws Exception {
        // Given
        AssignmentDto newAssignment = new AssignmentDto();
        newAssignment.setDescription("Test");
        newAssignment.setPriority(MessagePriority.High.toString());
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAssignment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignment created successfully")))
                .andExpect(jsonPath("$.data", is("123")));
    }

    @DisplayName("Create Assignment: Failure")
    @WithMockUser
    @Test
    void givenInvalidData_whenCreateAssignment_thenReturnServerError() throws Exception {
        given(assignmentService.storeAssignment(any(AssignmentDto.class))).willReturn(null);

        // Mock JWT authentication
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);

        mockMvc.perform(post("/api/assignments/create")
                        .with(csrf())
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAssignment)))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Update Assignment: Success")
    @WithMockUser
    @Test
    void givenValidAssignmentIdAndDto_whenUpdateAssignment_thenReturnSuccess() throws Exception {
        // When - Then
        mockMvc.perform(put("/api/assignments/assignmentId")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAssignment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignment updated successfully!")));
    }

    @DisplayName("Update Assignment Comments: Success")
    @WithMockUser
    @Test
    void givenValidAssignmentIdAndCommentDto_whenUpdateAssignmentComments_thenReturnSuccess() throws Exception {
        mockMvc.perform(put("/api/assignments/assignmentId/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Assignment comments updated successfully!")));
    }

    @DisplayName("Update Assignment Comments: Validator Error")
    @WithMockUser
    @Test
    void givenAssignmentIdAndInvalidCommentDto_whenUpdateAssignmentComments_thenReturnError() throws Exception {
        AssignmentCommentDto invalidComment = new AssignmentCommentDto();

        mockMvc.perform(put("/api/assignments/assignmentId/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidComment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }
}

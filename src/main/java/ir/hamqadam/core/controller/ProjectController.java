package ir.hamqadam.core.controller;

import ir.hamqadam.core.controller.dto.common.MessageResponse;
import ir.hamqadam.core.controller.dto.common.PageableResponseDTO;
import ir.hamqadam.core.controller.dto.project.*;
import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.model.Project;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.ProjectService;
import ir.hamqadam.core.service.UserService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
// import org.modelmapper.ModelMapper;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;
    // private final ModelMapper modelMapper;

    @Autowired
    public ProjectController(ProjectService projectService, UserService userService /*, ModelMapper modelMapper*/) {
        this.projectService = projectService;
        this.userService = userService;
        // this.modelMapper = modelMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectResponseDTO> createProject(
            @Valid @RequestBody ProjectCreationRequestDTO creationRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        User actingUser = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));

        // Map DTO's creatorInfo to model's CreatorInfo
        Project.CreatorInfo modelCreatorInfo = new Project.CreatorInfo(
                creationRequest.getCreatorInfo().getCreatorType(),
                creationRequest.getCreatorInfo().getCreatorId(),
                actingUser.getUserId() // acting user is always the authenticated one
        );

        Project newProject = projectService.createProject(
                creationRequest.getProjectName(),
                creationRequest.getProjectHandle(),
                creationRequest.getDescriptivePostId(),
                modelCreatorInfo,
                creationRequest.getVisibility(),
                creationRequest.getInitialStatus(),
                creationRequest.getManagingTeamIds(),
                actingUser
        );
        return new ResponseEntity<>(convertToProjectResponseDTO(newProject), HttpStatus.CREATED);
    }

    @GetMapping("/{projectIdOrHandle}")
    public ResponseEntity<ProjectResponseDTO> getProjectDetails(@PathVariable String projectIdOrHandle) {
        Project project = projectService.findProjectById(projectIdOrHandle)
                .orElseGet(() -> projectService.findProjectByHandle(projectIdOrHandle)
                        .orElseThrow(() -> new ResourceNotFoundException("Project", "identifier", projectIdOrHandle)));
        // Add logic here to check visibility if project is private, based on current user (if any)
        return ResponseEntity.ok(convertToProjectResponseDTO(project));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("@projectSecurityService.canUpdateProjectInfo(#projectId, principal.username)")
    public ResponseEntity<ProjectResponseDTO> updateProjectInfo(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectUpdateRequestDTO updateRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Project updatedProject = projectService.updateProjectInfo(
                projectId,
                updateRequest.getProjectName(),
                updateRequest.getVisibility(),
                updateRequest.getProjectGoals(),
                updateRequest.getProjectScope(),
                // ... pass other fields from DTO to service
                currentUserDetails.getUsername() // Or User ID
        );
        return ResponseEntity.ok(convertToProjectResponseDTO(updatedProject));
    }

    @PutMapping("/{projectId}/status")
    @PreAuthorize("@projectSecurityService.canChangeProjectStatus(#projectId, principal.username)")
    public ResponseEntity<ProjectResponseDTO> changeProjectStatus(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectStatusUpdateRequestDTO statusRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Project updatedProject = projectService.changeProjectStatus(
                projectId,
                statusRequest.getNewStatus(),
                currentUserDetails.getUsername() // Or User ID
        );
        return ResponseEntity.ok(convertToProjectResponseDTO(updatedProject));
    }

    @PostMapping("/{projectId}/teams")
    @PreAuthorize("@projectSecurityService.canManageProjectTeams(#projectId, principal.username)")
    public ResponseEntity<ProjectResponseDTO> addTeamToProject(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectTeamManagementRequestDTO teamRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Project updatedProject = projectService.addTeamToProject(
                projectId,
                teamRequest.getTeamId(),
                teamRequest.isManagingTeam(),
                teamRequest.getRoleInProject(),
                currentUserDetails.getUsername()
        );
        return ResponseEntity.ok(convertToProjectResponseDTO(updatedProject));
    }

    @DeleteMapping("/{projectId}/teams/{teamId}")
    @PreAuthorize("@projectSecurityService.canManageProjectTeams(#projectId, principal.username)")
    public ResponseEntity<ProjectResponseDTO> removeTeamFromProject(
            @PathVariable String projectId,
            @PathVariable String teamId,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Project updatedProject = projectService.removeTeamFromProject(
                projectId,
                teamId,
                currentUserDetails.getUsername()
        );
        return ResponseEntity.ok(convertToProjectResponseDTO(updatedProject));
    }

    @PostMapping("/{projectId}/contributors/individual")
    @PreAuthorize("@projectSecurityService.canManageProjectContributors(#projectId, principal.username)")
    public ResponseEntity<ProjectResponseDTO> addIndividualContributor(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectIndividualContributorRequestDTO contributorRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Project updatedProject = projectService.addIndividualContributorToProject(
                projectId,
                contributorRequest.getUserId(),
                contributorRequest.getRoleInProject(),
                contributorRequest.getContributionDescription(),
                currentUserDetails.getUsername()
        );
        return ResponseEntity.ok(convertToProjectResponseDTO(updatedProject));
    }

    @DeleteMapping("/{projectId}/contributors/individual/{userId}")
    @PreAuthorize("@projectSecurityService.canManageProjectContributors(#projectId, principal.username)")
    public ResponseEntity<ProjectResponseDTO> removeIndividualContributor(
            @PathVariable String projectId,
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Project updatedProject = projectService.removeIndividualContributorFromProject(
                projectId,
                userId,
                currentUserDetails.getUsername()
        );
        return ResponseEntity.ok(convertToProjectResponseDTO(updatedProject));
    }

    @GetMapping("/search")
    public ResponseEntity<PageableResponseDTO<ProjectResponseDTO>> searchPublicProjects(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "projectName.en") Pageable pageable) {
        Page<Project> projectPage = projectService.searchPublicProjects(query, pageable);
        Page<ProjectResponseDTO> dtoPage = projectPage.map(this::convertToProjectResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    @GetMapping("/by-team/{teamId}")
    public ResponseEntity<PageableResponseDTO<ProjectResponseDTO>> getProjectsByTeam(
            @PathVariable String teamId,
            @RequestParam(defaultValue = "managing") String type, // "managing" or "contributing"
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUserDetails) { // To check visibility if needed
        Page<Project> projectPage;
        if ("contributing".equalsIgnoreCase(type)) {
            projectPage = projectService.findProjectsByContributingTeam(teamId, pageable);
        } else {
            projectPage = projectService.findProjectsByManagingTeam(teamId, pageable);
        }
        // Further filter by visibility based on currentUserDetails if necessary for private projects
        Page<ProjectResponseDTO> dtoPage = projectPage.map(this::convertToProjectResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    // --- Placeholder DTO Conversion ---
    private ProjectResponseDTO convertToProjectResponseDTO(Project project) {
        if (project == null) return null;
        // Use ModelMapper or MapStruct in a real application
        return ProjectResponseDTO.builder()
                .projectId(project.getProjectId())
                .projectName(project.getProjectName())
                .projectHandle(project.getProjectHandle())
                .descriptivePostId(project.getDescriptivePostId())
                .creatorInfo(project.getCreatorInfo()) // Assuming model's inner class is fine for DTO
                .status(project.getStatus())
                .visibility(project.getVisibility())
                .startDate(project.getStartDate())
                .endDateOrDeadline(project.getEndDateOrDeadline())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .projectGoals(project.getProjectGoals())
                .projectScope(project.getProjectScope())
                .projectDeliverables(project.getProjectDeliverables())
                .managingTeamIds(project.getManagingTeamIds())
                .contributingTeams(project.getContributingTeams()) // Assuming model's inner class is fine
                .individualContributors(project.getIndividualContributors()) // Assuming model's inner class is fine
                .projectPlanLinkOrData(project.getProjectPlanLinkOrData())
                .projectResourcesLinks(project.getProjectResourcesLinks())
                .communicationChannels(project.getCommunicationChannels())
                .linkedRoutineIds(project.getLinkedRoutineIds())
                .projectUpdatesPostIds(project.getProjectUpdatesPostIds())
                .build();
    }
}
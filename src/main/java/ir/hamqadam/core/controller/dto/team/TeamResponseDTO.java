package ir.hamqadam.core.controller.dto.team;

import ir.hamqadam.core.model.Team; // For enums
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponseDTO {
    private String teamId;
    private Map<String, String> teamName; // i18n
    private String teamHandle;
    private String creatorUserId;
    private String introductoryPostId;
    private Map<String, String> description; // i18n
    private String profilePictureUrl;
    private String coverPictureUrl;
    private List<TeamMemberDTO> members; // Using the DTO for members
    private Team.TeamVisibility visibility;
    private boolean membershipApprovalRequired;
    private Team.TeamStatus teamStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional: Links to projects, routines etc. could be summary counts or separate endpoints
    private Long projectCount;
    private Long routineCount;
}
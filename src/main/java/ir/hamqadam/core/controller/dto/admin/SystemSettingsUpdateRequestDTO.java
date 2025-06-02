package ir.hamqadam.core.controller.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class SystemSettingsUpdateRequestDTO {
    @NotEmpty
    @Valid
    private List<SystemSettingDTO> settings;
}
package ir.hamqadam.core.controller.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingDTO {
    @NotBlank
    private String key;
    @NotBlank
    private String value;
    private Map<String, String> description; // Optional
}
package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.StageUpsertRequest;
import com.hospital.followup.dto.admin.StageView;
import com.hospital.followup.service.StageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stages")
public class AdminStageController {

    private final StageService stageService;

    public AdminStageController(StageService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    public ApiResponse<List<StageView>> listStages() {
        return ApiResponse.ok(stageService.listStages());
    }

    @PostMapping
    public ApiResponse<StageView> createStage(@Valid @RequestBody StageUpsertRequest request) {
        return ApiResponse.ok(stageService.createStage(request), "阶段创建成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<StageView> updateStage(@PathVariable Long id, @Valid @RequestBody StageUpsertRequest request) {
        return ApiResponse.ok(stageService.updateStage(id, request), "阶段更新成功");
    }
}

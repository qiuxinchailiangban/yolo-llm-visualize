package com.hospital.followup.service;

import com.hospital.followup.domain.FollowupStage;
import com.hospital.followup.dto.admin.StageUpsertRequest;
import com.hospital.followup.dto.admin.StageView;
import com.hospital.followup.repository.FollowupStageRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StageService {

    private final FollowupStageRepository stageRepository;

    public StageService(FollowupStageRepository stageRepository) {
        this.stageRepository = stageRepository;
    }

    @Transactional(readOnly = true)
    public List<StageView> listStages() {
        return stageRepository.findAll().stream()
            .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
            .map(this::toView)
            .toList();
    }

    @Transactional
    public StageView createStage(StageUpsertRequest request) {
        if (stageRepository.findByStageCode(request.stageCode()).isPresent()) {
            throw new IllegalArgumentException("阶段编码已存在");
        }
        FollowupStage stage = new FollowupStage();
        fillStage(stage, request);
        return toView(stageRepository.save(stage));
    }

    @Transactional
    public StageView updateStage(Long id, StageUpsertRequest request) {
        FollowupStage stage = stageRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("随访阶段不存在"));
        stageRepository.findByStageCode(request.stageCode())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("阶段编码已存在");
            });
        fillStage(stage, request);
        return toView(stageRepository.save(stage));
    }

    private void fillStage(FollowupStage stage, StageUpsertRequest request) {
        stage.setStageCode(request.stageCode());
        stage.setStageName(request.stageName());
        stage.setDayOffset(request.dayOffset());
        stage.setSortOrder(request.sortOrder());
        stage.setEnabled(request.enabled());
        stage.setReminderEnabled(request.reminderEnabled());
        stage.setDescription(request.description());
    }

    private StageView toView(FollowupStage stage) {
        return new StageView(
            stage.getId(),
            stage.getStageCode(),
            stage.getStageName(),
            stage.getDayOffset(),
            stage.getSortOrder(),
            stage.getEnabled(),
            stage.getReminderEnabled(),
            stage.getDescription()
        );
    }
}

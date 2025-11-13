package com.example.common.core.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 기본 DTO 클래스
 */
@Data
public abstract class BaseDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}


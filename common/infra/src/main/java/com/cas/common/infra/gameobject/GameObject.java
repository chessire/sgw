package com.cas.common.infra.gameobject;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 게임 오브젝트 추상 클래스
 * 모든 게임 오브젝트는 이 클래스를 상속받아야 합니다.
 */
public abstract class GameObject implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 오브젝트 고유 ID
     */
    protected String objectId;
    
    /**
     * 오브젝트 타입
     */
    protected String objectType;
    
    /**
     * 생성 시간
     */
    protected LocalDateTime createdAt;
    
    /**
     * 마지막 업데이트 시간
     */
    protected LocalDateTime updatedAt;
    
    /**
     * 오브젝트가 활성화되어 있는지 여부
     */
    protected boolean active;
    
    /**
     * GameObject 생성자
     */
    protected GameObject() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }
    
    /**
     * 오브젝트를 초기화합니다.
     */
    public abstract void initialize();
    
    /**
     * 오브젝트를 업데이트합니다.
     */
    public void update() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 오브젝트를 JSON 직렬화를 위한 문자열로 변환합니다.
     */
    public abstract String toJsonString();
    
    // Getters and Setters
    
    public String getObjectId() {
        return objectId;
    }
    
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }
    
    public String getObjectType() {
        return objectType;
    }
    
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
}


package com.example.common.infra.gameobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Random;
import java.util.UUID;

/**
 * 테스트용 게임 오브젝트
 */
public class TestObject extends GameObject {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 플레이어 이름
     */
    private String playerName;
    
    /**
     * 레벨
     */
    private int level;
    
    /**
     * 체력
     */
    private int health;
    
    /**
     * 마나
     */
    private int mana;
    
    /**
     * X 좌표
     */
    private double positionX;
    
    /**
     * Y 좌표
     */
    private double positionY;
    
    /**
     * Z 좌표
     */
    private double positionZ;
    
    /**
     * 점수
     */
    private long score;
    
    /**
     * 경험치
     */
    private long experience;
    
    /**
     * 골드
     */
    private int gold;
    
    public TestObject() {
        super();
        this.objectType = "TestObject";
    }
    
    @Override
    public void initialize() {
        Random random = new Random();
        
        this.objectId = UUID.randomUUID().toString();
        this.playerName = generateRandomName();
        this.level = random.nextInt(100) + 1; // 1-100
        this.health = random.nextInt(1000) + 100; // 100-1100
        this.mana = random.nextInt(500) + 50; // 50-550
        this.positionX = random.nextDouble() * 1000 - 500; // -500 ~ 500
        this.positionY = random.nextDouble() * 1000 - 500; // -500 ~ 500
        this.positionZ = random.nextDouble() * 100; // 0 ~ 100
        this.score = random.nextLong() % 1000000; // 0-999999
        this.experience = random.nextLong() % 100000; // 0-99999
        this.gold = random.nextInt(10000); // 0-9999
        
        update();
    }
    
    /**
     * 랜덤 플레이어 이름 생성
     */
    private String generateRandomName() {
        String[] prefixes = {"Dark", "Shadow", "Light", "Fire", "Ice", "Thunder", "Storm", "Dragon", "Phoenix", "Knight"};
        String[] suffixes = {"Warrior", "Mage", "Hunter", "Slayer", "Master", "Lord", "King", "Queen", "Hero", "Legend"};
        
        Random random = new Random();
        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        int number = random.nextInt(999) + 1;
        
        return prefix + suffix + number;
    }
    
    @Override
    public String toJsonString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize TestObject to JSON", e);
        }
    }
    
    /**
     * JSON 문자열로부터 TestObject 생성
     * 
     * @deprecated GameObjectParser.parse()를 사용하세요
     */
    @Deprecated
    public static TestObject fromJsonString(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            return mapper.readValue(json, TestObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize TestObject from JSON", e);
        }
    }
    
    // Getters and Setters
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getHealth() {
        return health;
    }
    
    public void setHealth(int health) {
        this.health = health;
    }
    
    public int getMana() {
        return mana;
    }
    
    public void setMana(int mana) {
        this.mana = mana;
    }
    
    public double getPositionX() {
        return positionX;
    }
    
    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }
    
    public double getPositionY() {
        return positionY;
    }
    
    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }
    
    public double getPositionZ() {
        return positionZ;
    }
    
    public void setPositionZ(double positionZ) {
        this.positionZ = positionZ;
    }
    
    public long getScore() {
        return score;
    }
    
    public void setScore(long score) {
        this.score = score;
    }
    
    public long getExperience() {
        return experience;
    }
    
    public void setExperience(long experience) {
        this.experience = experience;
    }
    
    public int getGold() {
        return gold;
    }
    
    public void setGold(int gold) {
        this.gold = gold;
    }
    
    @Override
    public String toString() {
        return "TestObject{" +
                "objectId='" + objectId + '\'' +
                ", playerName='" + playerName + '\'' +
                ", level=" + level +
                ", health=" + health +
                ", mana=" + mana +
                ", position=(" + positionX + ", " + positionY + ", " + positionZ + ")" +
                ", score=" + score +
                ", experience=" + experience +
                ", gold=" + gold +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", active=" + active +
                '}';
    }
}


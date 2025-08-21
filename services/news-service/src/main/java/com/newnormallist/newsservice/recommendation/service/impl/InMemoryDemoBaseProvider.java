package com.newnormallist.newsservice.recommendation.service.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;

import com.newnormallist.newsservice.recommendation.entity.Category;
import com.newnormallist.newsservice.recommendation.entity.AgeBucket;

import com.newnormallist.newsservice.recommendation.service.DemoBaseProvider;

// 초기엔 하드코딩 테이블로 D(c) 제공
// 추후 DB 기반 Provider로 교체 가능
public class InMemoryDemoBaseProvider implements DemoBaseProvider {

    private final Map<String, Map<Category, Double>> table = new HashMap<>();

    public InMemoryDemoBaseProvider() {
        // 연령/성별 조합별 기본분포 채우기(합=1)
        
        // 남성 20대: IT_SCIENCE 0.22, VEHICLE 0.15, INTERNATIONAL 0.12, ECONOMY 0.12, CULTURE 0.10, SOCIETY 0.09, POLITICS 0.08, TRAVEL_FOOD 0.07, ART 0.05
        put(AgeBucket.AGE_20s, "MALE", Map.of(
            Category.IT_SCIENCE, 0.19,
            Category.SOCIETY, 0.17,
            Category.ECONOMY, 0.12,
            Category.VEHICLE, 0.12,
            Category.INTERNATIONAL, 0.10,
            Category.POLITICS, 0.10,
            Category.CULTURE, 0.08,
            Category.TRAVEL_FOOD, 0.07,
            Category.ART, 0.06
        ));
        
        // 여성 20대: CULTURE 0.20, TRAVEL_FOOD 0.18, ART 0.12, IT_SCIENCE 0.11, ECONOMY 0.11, SOCIETY 0.10, INTERNATIONAL 0.08, POLITICS 0.06, VEHICLE 0.04
        put(AgeBucket.AGE_20s, "FEMALE", Map.of(
            Category.SOCIETY, 0.20,
            Category.IT_SCIENCE, 0.18,
            Category.ART, 0.12,
            Category.CULTURE, 0.11,
            Category.ECONOMY, 0.10,
            Category.POLITICS, 0.10,
            Category.INTERNATIONAL, 0.08,
            Category.TRAVEL_FOOD, 0.06,
            Category.VEHICLE, 0.04
        ));
        
        // 남성 30대: ECONOMY 0.18, IT_SCIENCE 0.16, POLITICS 0.13, SOCIETY 0.12, INTERNATIONAL 0.11, VEHICLE 0.10, CULTURE 0.08, TRAVEL_FOOD 0.07, ART 0.05
        put(AgeBucket.AGE_30s, "MALE", Map.of(
            Category.ECONOMY, 0.18,
            Category.IT_SCIENCE, 0.16,
            Category.POLITICS, 0.13,
            Category.SOCIETY, 0.12,
            Category.INTERNATIONAL, 0.11,
            Category.VEHICLE, 0.10,
            Category.CULTURE, 0.08,
            Category.TRAVEL_FOOD, 0.07,
            Category.ART, 0.05
        ));
        
        // 여성 30대: CULTURE 0.16, ECONOMY 0.15, TRAVEL_FOOD 0.14, SOCIETY 0.13, ART 0.10, INTERNATIONAL 0.10, IT_SCIENCE 0.09, POLITICS 0.08, VEHICLE 0.05
        put(AgeBucket.AGE_30s, "FEMALE", Map.of(
            Category.CULTURE, 0.16,
            Category.ECONOMY, 0.15,
            Category.TRAVEL_FOOD, 0.14,
            Category.SOCIETY, 0.13,
            Category.ART, 0.10,
            Category.INTERNATIONAL, 0.10,
            Category.IT_SCIENCE, 0.09,
            Category.POLITICS, 0.08,
            Category.VEHICLE, 0.05
        ));
        
        // 남성 40대: POLITICS 0.18, ECONOMY 0.17, SOCIETY 0.14, INTERNATIONAL 0.12, IT_SCIENCE 0.10, VEHICLE 0.09, CULTURE 0.08, TRAVEL_FOOD 0.07, ART 0.05
        put(AgeBucket.AGE_40s, "MALE", Map.of(
            Category.POLITICS, 0.18,
            Category.ECONOMY, 0.17,
            Category.SOCIETY, 0.14,
            Category.INTERNATIONAL, 0.12,
            Category.IT_SCIENCE, 0.10,
            Category.VEHICLE, 0.09,
            Category.CULTURE, 0.08,
            Category.TRAVEL_FOOD, 0.07,
            Category.ART, 0.05
        ));
        
        // 여성 40대: SOCIETY 0.17, CULTURE 0.15, ECONOMY 0.14, POLITICS 0.13, INTERNATIONAL 0.11, TRAVEL_FOOD 0.10, ART 0.08, IT_SCIENCE 0.07, VEHICLE 0.05
        put(AgeBucket.AGE_40s, "FEMALE", Map.of(
            Category.SOCIETY, 0.17,
            Category.CULTURE, 0.15,
            Category.ECONOMY, 0.14,
            Category.POLITICS, 0.13,
            Category.INTERNATIONAL, 0.11,
            Category.TRAVEL_FOOD, 0.10,
            Category.ART, 0.08,
            Category.IT_SCIENCE, 0.07,
            Category.VEHICLE, 0.05
        ));
        
        // 남성 50대+: POLITICS 0.22, SOCIETY 0.18, INTERNATIONAL 0.15, ECONOMY 0.14, CULTURE 0.09, IT_SCIENCE 0.08, VEHICLE 0.06, TRAVEL_FOOD 0.05, ART 0.03
        put(AgeBucket.AGE_50s, "MALE", Map.of(
            Category.POLITICS, 0.22,
            Category.SOCIETY, 0.18,
            Category.INTERNATIONAL, 0.15,
            Category.ECONOMY, 0.14,
            Category.CULTURE, 0.09,
            Category.IT_SCIENCE, 0.08,
            Category.VEHICLE, 0.06,
            Category.TRAVEL_FOOD, 0.05,
            Category.ART, 0.03
        ));
        
        // 여성 50대+: POLITICS 0.18, SOCIETY 0.18, CULTURE 0.16, INTERNATIONAL 0.13, ECONOMY 0.12, ART 0.09, TRAVEL_FOOD 0.07, IT_SCIENCE 0.05, VEHICLE 0.02
        put(AgeBucket.AGE_50s, "FEMALE", Map.of(
            Category.POLITICS, 0.18,
            Category.SOCIETY, 0.18,
            Category.CULTURE, 0.16,
            Category.INTERNATIONAL, 0.13,
            Category.ECONOMY, 0.12,
            Category.ART, 0.09,
            Category.TRAVEL_FOOD, 0.07,
            Category.IT_SCIENCE, 0.05,
            Category.VEHICLE, 0.02
        ));

        // 남성 60대+: POLITICS 0.20, SOCIETY 0.18, INTERNATIONAL 0.15, ECONOMY 0.14, CULTURE 0.09, ART 0.07, TRAVEL_FOOD 0.06, IT_SCIENCE 0.05, VEHICLE 0.01
        put(AgeBucket.AGE_60s_PLUS, "MALE", Map.of(
            Category.POLITICS, 0.20,
            Category.SOCIETY, 0.18,
            Category.INTERNATIONAL, 0.15,
            Category.ECONOMY, 0.14,
            Category.CULTURE, 0.09,
            Category.ART, 0.07,
            Category.TRAVEL_FOOD, 0.06,
            Category.IT_SCIENCE, 0.05,
            Category.VEHICLE, 0.01
        ));

        // 여성 60대+: SOCIETY 0.18, POLITICS 0.16, CULTURE 0.15, INTERNATIONAL 0.14, ECONOMY 0.12, ART 0.09, TRAVEL_FOOD 0.07, IT_SCIENCE 0.05, VEHICLE 0.02
        put(AgeBucket.AGE_60s_PLUS, "FEMALE", Map.of(
            Category.SOCIETY, 0.18,
            Category.POLITICS, 0.16,
            Category.CULTURE, 0.15,
            Category.INTERNATIONAL, 0.14,
            Category.ECONOMY, 0.12,
            Category.ART, 0.09,
            Category.TRAVEL_FOOD, 0.07,
            Category.IT_SCIENCE, 0.05,
            Category.VEHICLE, 0.02
        ));
    }
    private void put(AgeBucket a, String g, Map<Category,Double> m){ table.put(key(a,g), m); }
    private String key(AgeBucket a, String g){ return a.name()+"|"+g; }

    @Override public Map<Category, Double> getBase(AgeBucket age, String gender) {
        return table.getOrDefault(key(age, gender), uniform());
    }
    private Map<Category, Double> uniform(){
        Map<Category, Double> m = new EnumMap<>(Category.class);
        for (Category c: Category.values()) m.put(c, 1.0/Category.values().length);
        return m;
    }
}
package com.wardk.meeteam_backend.acceptance.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 테스트 간 데이터 격리를 위한 DB 초기화 유틸리티
 */
@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @Transactional
    public void execute() {
        entityManager.flush();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        for (String tableName : getTableNames()) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        }

        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }

    private List<String> getTableNames() {
        if (tableNames == null) {
            tableNames = entityManager.getMetamodel().getEntities().stream()
                    .map(this::getTableName)
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toList());
        }
        return tableNames;
    }

    private String getTableName(EntityType<?> entity) {
        Class<?> javaType = entity.getJavaType();
        Table tableAnnotation = javaType.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }

        return camelToSnake(entity.getName());
    }

    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
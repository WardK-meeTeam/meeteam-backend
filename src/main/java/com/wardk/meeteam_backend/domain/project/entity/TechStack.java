package com.wardk.meeteam_backend.domain.project.entity;

public enum TechStack {
    ANSIBLE("Ansible"),
    AWS("AWS"),
    DOCKER("Docker"),
    ELASTICSEARCH("Elasticsearch"),
    EXPRESS("Express"),
    GIT("Git"),
    GITHUB_ACTIONS("GitHub Actions"),
    GRADLE("Gradle"),
    GRAFANA("Grafana"),
    GRAPHQL("GraphQL"),
    GRPC("gRPC"),
    HIBERNATE("Hibernate"),
    HTML_CSS("HTML/CSS"),
    JAVA("Java"),
    JAVASCRIPT("JavaScript"),
    JENKINS("Jenkins"),
    JPA("JPA"),
    JUNIT5("JUnit 5"),
    JWT("JWT"),
    KAFKA("Kafka"),
    KIBANA("Kibana"),
    KOTLIN("Kotlin"),
    KUBERNETES("Kubernetes"),
    LINUX("Linux"),
    LOGSTASH("Logstash"),
    MAVEN("Maven"),
    MICROMETER("Micrometer"),
    MOCKITO("Mockito"),
    MONGODB("MongoDB"),
    MYSQL("MySQL"),
    NEXT_JS("Next.js"),
    NGINX("Nginx"),
    NODE_JS("Node.js"),
    OAUTH2("OAuth2"),
    OPENAPI_SWAGGER("OpenAPI/Swagger"),
    POSTGRESQL("PostgreSQL"),
    PROMETHEUS("Prometheus"),
    PYTHON("Python"),
    QUERYDSL("QueryDSL"),
    RABBITMQ("RabbitMQ"),
    REACT("React"),
    REDIS("Redis"),
    SPRING("Spring"),
    SPRING_BOOT("Spring Boot"),
    SSE("SSE"),
    TAILWIND_CSS("Tailwind CSS"),
    TERRAFORM("Terraform"),
    TYPESCRIPT("TypeScript"),
    WEBFLUX("WebFlux"),
    WEBSOCKET("WebSocket");

    private final String techName;

    TechStack(String techName) {
        this.techName = techName;
    }

    public String getTechName() {
        return techName;
    }

    // name 으로 Enum 찾기
    public static TechStack fromName(String name) {
        for (TechStack stack : values()) {
            if (stack.techName.equalsIgnoreCase(name)) {
                return stack;
            }
        }
        throw new IllegalArgumentException("Invalid tech name: " + name);
    }
}
package com.wardk.meeteam_backend.domain.jobcatalog.bootstrap;

import com.wardk.meeteam_backend.domain.jobcatalog.entity.JobField;
import com.wardk.meeteam_backend.domain.jobcatalog.entity.JobFieldTechStack;
import com.wardk.meeteam_backend.domain.jobcatalog.entity.JobPosition;
import com.wardk.meeteam_backend.domain.jobcatalog.entity.TechStack;
import com.wardk.meeteam_backend.domain.jobcatalog.repository.JobFieldRepository;
import com.wardk.meeteam_backend.domain.jobcatalog.repository.JobFieldTechStackRepository;
import com.wardk.meeteam_backend.domain.jobcatalog.repository.JobPositionRepository;
import com.wardk.meeteam_backend.domain.jobcatalog.repository.TechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobDataInitializer implements ApplicationRunner {

    private final JobFieldRepository jobFieldRepository;
    private final JobPositionRepository jobPositionRepository;
    private final TechStackRepository techStackRepository;
    private final JobFieldTechStackRepository jobFieldTechStackRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (FieldSeed fieldSeed : FIELD_SEEDS) {
            JobField field = jobFieldRepository.findByCode(fieldSeed.code())
                    .orElseGet(() -> jobFieldRepository.save(
                            JobField.of(fieldSeed.code(), fieldSeed.name())
                    ));

            for (PositionSeed positionSeed : fieldSeed.positions()) {
                jobPositionRepository.findByCode(positionSeed.code())
                        .orElseGet(() -> jobPositionRepository.save(
                                JobPosition.of(field, positionSeed.code(), positionSeed.name())
                        ));
            }

            for (String stackName : fieldSeed.techStacks()) {
                TechStack techStack = techStackRepository.findByName(stackName)
                        .orElseGet(() -> techStackRepository.save(TechStack.of(stackName)));

                if (!jobFieldTechStackRepository.existsByJobFieldIdAndTechStackId(field.getId(), techStack.getId())) {
                    jobFieldTechStackRepository.save(JobFieldTechStack.of(field, techStack));
                }
            }
        }
    }

    private static final List<FieldSeed> FIELD_SEEDS = List.of(
            new FieldSeed(
                    "PLANNING",
                    "기획",
                    List.of(
                            new PositionSeed("PRODUCT_MANAGER", "PM 프로덕트 매니저"),
                            new PositionSeed("PRODUCT_OWNER", "PO 프로덕트 오너"),
                            new PositionSeed("SERVICE_PLANNER", "서비스 기획")
                    ),
                    List.of("Notion", "Jira", "Figma", "Google Analytics")
            ),
            new FieldSeed(
                    "DESIGN",
                    "디자인",
                    List.of(
                            new PositionSeed("UI_UX_DESIGNER", "UI/UX 디자이너"),
                            new PositionSeed("MOTION_DESIGNER", "모션 디자이너"),
                            new PositionSeed("BX_BRAND_DESIGNER", "BX 브랜드 디자이너")
                    ),
                    List.of("Figma", "Zeplin", "After Effects", "Premiere", "Illustrator", "Photoshop")
            ),
            new FieldSeed(
                    "FRONTEND",
                    "프론트",
                    List.of(
                            new PositionSeed("WEB_FRONTEND", "웹 프론트엔드"),
                            new PositionSeed("IOS", "iOS"),
                            new PositionSeed("ANDROID", "Android"),
                            new PositionSeed("CROSS_PLATFORM", "크로스 플랫폼")
                    ),
                    List.of(
                            "React.js", "TypeScript", "Next.js", "Vue.js", "Angular", "Zustand", "Redux", "TailwindCSS",
                            "Swift", "SwiftUI", "UIKit", "Firebase", "Kotlin", "Jetpack Compose", "Coroutine", "Hilt",
                            "React Native", "Expo", "Dart", "Flutter"
                    )
            ),
            new FieldSeed(
                    "BACKEND",
                    "백엔드",
                    List.of(
                            new PositionSeed("JAVA_SPRING", "Java/Spring"),
                            new PositionSeed("KOTLIN_SPRING", "Kotlin/Spring"),
                            new PositionSeed("NODE_NEXTJS", "Node/NextJS"),
                            new PositionSeed("PYTHON_BACKEND", "Python Backend")
                    ),
                    List.of(
                            "Java", "Kotlin", "TypeScript", "Python", "Ruby on Rails", "Spring Boot", "Spring WebFlux",
                            "JPA", "QueryDSL", "MySQL", "PostgreSQL", "MariaDB", "MongoDB", "Redis", "ElasticSearch",
                            "Docker", "AWS", "Nginx", "Linux", "GitHub Actions", "GitLab CI/CD", "Jenkins",
                            "Prometheus", "Grafana", "WebSocket", "Kafka", "RabbitMQ", "NestJs", "Node.js"
                    )
            ),
            new FieldSeed(
                    "AI",
                    "AI",
                    List.of(
                            new PositionSeed("MACHINE_LEARNING", "머신 러닝"),
                            new PositionSeed("DEEP_LEARNING", "딥러닝"),
                            new PositionSeed("LLM", "LLM"),
                            new PositionSeed("MLOPS", "MLOps")
                    ),
                    List.of(
                            "Python", "NumPy", "Pandas", "Scikit-learn", "PyTorch", "TensorFlow", "CUDA", "Hugging Face",
                            "OpenAI API", "LangChain", "RAG", "Vector DB", "MLflow", "Airflow", "Docker", "Kubernetes", "AWS"
                    )
            ),
            new FieldSeed(
                    "INFRA_OPERATION",
                    "인프라/운영",
                    List.of(
                            new PositionSeed("DEVOPS_ARCHITECT", "DevOps 엔지니어/아키텍처"),
                            new PositionSeed("QA", "QA"),
                            new PositionSeed("CLOUD_ENGINEER", "Cloud 엔지니어")
                    ),
                    List.of(
                            "AWS", "Docker", "Kubernetes", "Nginx", "GitHub Actions", "Prometheus/Grafana", "Postman",
                            "Jira", "Playwright", "Selenium", "Cypress", "EC2", "RDS", "S3", "IAM"
                    )
            )
    );

    private record FieldSeed(
            String code,
            String name,
            List<PositionSeed> positions,
            List<String> techStacks
    ) {
    }

    private record PositionSeed(
            String code,
            String name
    ) {
    }
}

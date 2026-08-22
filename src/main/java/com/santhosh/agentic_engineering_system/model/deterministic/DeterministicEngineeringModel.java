package com.santhosh.agentic_engineering_system.model.deterministic;

import com.santhosh.agentic_engineering_system.model.DocumentationProposal;
import com.santhosh.agentic_engineering_system.model.EngineeringModel;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.EngineeringTaskPlan;
import com.santhosh.agentic_engineering_system.model.FileChangeType;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.model.ProposedFileChange;
import com.santhosh.agentic_engineering_system.model.RequirementAnalysis;
import com.santhosh.agentic_engineering_system.model.RequirementContext;
import com.santhosh.agentic_engineering_system.model.ScenarioType;
import com.santhosh.agentic_engineering_system.model.ValidationFailure;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(
        name = "agentic.model.provider",
        havingValue = "deterministic",
        matchIfMissing = true
)
public class DeterministicEngineeringModel implements EngineeringModel {

    @Override
    public RequirementAnalysis analyzeRequirement(RequirementContext context) {
        String requirement = context.rawRequirement().trim();
        boolean ambiguous = context.clarificationHistory().isEmpty() &&
                (context.scenarioType() == ScenarioType.AMBIGUOUS ||
                        isVague(requirement));
        String normalized = context.clarificationHistory().isEmpty()
                ? requirement
                : requirement + " Clarifications: " +
                        String.join("; ", context.clarificationHistory());
        return new RequirementAnalysis(
                normalized,
                ambiguous ? List.of() : List.of(
                        "The requested behavior is implemented",
                        "Existing behavior remains compatible",
                        "Generated unit and integration tests pass",
                        "The resulting source diff is reviewable"
                ),
                ambiguous ? List.of(
                        "Which measurable behavior must change?",
                        "What acceptance criteria define success?"
                ) : List.of(),
                List.of("The repository build conventions remain authoritative"),
                List.of("Generated changes may introduce regressions"),
                ambiguous
        );
    }

    @Override
    public EngineeringPlan createPlan(RepositoryContext repository) {
        return new EngineeringPlan(
                "Ground the change in the repository map, generate production " +
                        "and test changes in parallel, validate the synchronized " +
                        "patch, document evidence, and request human approval.",
                List.of(
                        task("repository", "Repository reasoning",
                                "Confirm impacted APIs, services, persistence and tests",
                                List.of(), false, false),
                        task("implementation", "Implementation",
                                "Generate production source changes",
                                List.of("repository"), true, false),
                        task("tests", "Test generation",
                                "Generate acceptance and regression tests",
                                List.of("repository"), true, false),
                        task("validation", "Validation",
                                "Compile and test the combined patch",
                                List.of("implementation", "tests"), false, false),
                        task("documentation", "Documentation",
                                "Summarize rationale, evidence, risks and limitations",
                                List.of("validation"), false, false),
                        task("approval", "Release readiness",
                                "Require human review of the validated diff",
                                List.of("documentation"), false, true)
                ),
                List.of("Generated code may not match implicit conventions"),
                List.of("A bounded change favors reviewability over broad redesign")
        );
    }

    @Override
    public PatchProposal generateImplementation(
            EngineeringPlan plan,
            RepositoryContext repository
    ) {
        if (isGreenfield(repository)) return greenfieldImplementation();
        if (isAnalytics(repository)) return analyticsImplementation();
        String generatedStatus = repository.requirement()
                .toLowerCase(Locale.ROOT).contains("repair scenario")
                ? "BROKEN_AGENT_OUTPUT" : "implemented";
        return new PatchProposal(
                "Generate a bounded implementation marker for the requested change",
                List.of(new ProposedFileChange(
                        FileChangeType.CREATE,
                        "src/main/java/generated/AgentGeneratedChange.java",
                        null,
                        """
                        package generated;

                        public final class AgentGeneratedChange {
                            private AgentGeneratedChange() {}
                            public static String status() { return "%s"; }
                        }
                        """.formatted(generatedStatus),
                        "Provides deterministic source output for offline orchestration tests"
                )),
                List.of("The scenario fixture permits the generated package"),
                List.of("Scenario-specific output is introduced with executable fixtures")
        );
    }

    @Override
    public PatchProposal generateTests(
            EngineeringPlan plan,
            PatchProposal implementation,
            RepositoryContext repository
    ) {
        if (isGreenfield(repository)) return greenfieldTests();
        if (isAnalytics(repository)) return analyticsTests();
        return new PatchProposal(
                "Generate a deterministic acceptance test",
                List.of(new ProposedFileChange(
                        FileChangeType.CREATE,
                        "src/test/java/generated/AgentGeneratedChangeTest.java",
                        null,
                        """
                        package generated;

                        import org.junit.jupiter.api.Test;
                        import static org.assertj.core.api.Assertions.assertThat;

                        class AgentGeneratedChangeTest {
                            @Test
                            void reportsImplementedStatus() {
                                assertThat(AgentGeneratedChange.status())
                                        .isEqualTo("implemented");
                            }
                        }
                        """,
                        "Validates the deterministic generated production behavior"
                )),
                List.of("The fixture supplies JUnit and AssertJ"),
                List.of("Broader integration coverage depends on the scenario")
        );
    }

    @Override
    public PatchProposal repair(
            EngineeringPlan plan,
            PatchProposal previousPatch,
            ValidationFailure failure,
            RepositoryContext repository
    ) {
        List<ProposedFileChange> repaired = previousPatch.changes().stream()
                .map(change -> new ProposedFileChange(
                        change.type(),
                        change.path(),
                        change.expectedSha256(),
                        change.content() == null ? null : change.content()
                                .replace("BROKEN_AGENT_OUTPUT", "implemented"),
                        "Repaired using validation failure: " + failure.summary()
                ))
                .toList();
        return new PatchProposal(
                "Repair the previous patch using compiler and test evidence",
                repaired,
                previousPatch.assumptions(),
                previousPatch.risks()
        );
    }

    @Override
    public DocumentationProposal generateDocumentation(
            EngineeringPlan plan,
            PatchProposal validatedPatch,
            RepositoryContext repository
    ) {
        return new DocumentationProposal(
                "The agent generated and validated a bounded engineering change.",
                "The change follows the repository map and controlled tool boundaries.",
                List.of(
                        "The deterministic provider supports repeatable assessment fixtures",
                        "Arbitrary requirements require the configured LLM provider"
                )
        );
    }

    private EngineeringTaskPlan task(
            String id,
            String name,
            String description,
            List<String> dependencies,
            boolean parallel,
            boolean approval
    ) {
        return new EngineeringTaskPlan(
                id, name, description, dependencies, parallel, approval
        );
    }

    private boolean isVague(String requirement) {
        String lower = requirement.toLowerCase(Locale.ROOT);
        boolean vagueVerb = lower.contains("improve") ||
                lower.contains("enhance") || lower.contains("better");
        boolean measurable = lower.contains("total") || lower.contains("daily") ||
                lower.contains("endpoint") || lower.contains("count") ||
                lower.contains("status");
        return vagueVerb && !measurable;
    }

    private boolean isGreenfield(RepositoryContext repository) {
        return repository.requirement().toLowerCase(Locale.ROOT).contains("greenfield");
    }

    private boolean isAnalytics(RepositoryContext repository) {
        String value = repository.requirement().toLowerCase(Locale.ROOT);
        return value.contains("analytics") && !value.contains("repair scenario");
    }

    private PatchProposal greenfieldImplementation() {
        return new PatchProposal("Generate a complete Java 21 URL-shortener project",
                List.of(
                        create("pom.xml", """
                                <project xmlns="http://maven.apache.org/POM/4.0.0">
                                  <modelVersion>4.0.0</modelVersion>
                                  <groupId>generated</groupId><artifactId>url-shortener</artifactId><version>1.0.0</version>
                                  <properties><maven.compiler.release>21</maven.compiler.release><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
                                  <dependencies>
                                    <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><version>6.0.3</version><scope>test</scope></dependency>
                                    <dependency><groupId>org.assertj</groupId><artifactId>assertj-core</artifactId><version>3.27.7</version><scope>test</scope></dependency>
                                  </dependencies>
                                  <build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.5</version></plugin></plugins></build>
                                </project>
                                """, "Defines the generated project and executable test dependencies"),
                        create("src/main/java/generated/urlshortener/UrlShortener.java", """
                                package generated.urlshortener;

                                import java.net.URI;
                                import java.util.Optional;
                                import java.util.concurrent.ConcurrentHashMap;
                                import java.util.concurrent.atomic.AtomicLong;

                                public final class UrlShortener {
                                    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
                                    private final AtomicLong sequence = new AtomicLong(100_000);
                                    private final ConcurrentHashMap<String, URI> urls = new ConcurrentHashMap<>();

                                    public String shorten(URI target) {
                                        if (target == null || target.getScheme() == null ||
                                                !(target.getScheme().equals("http") || target.getScheme().equals("https"))) {
                                            throw new IllegalArgumentException("Only absolute HTTP(S) URLs are accepted");
                                        }
                                        String code = encode(sequence.incrementAndGet());
                                        urls.put(code, target);
                                        return code;
                                    }

                                    public Optional<URI> resolve(String code) { return Optional.ofNullable(urls.get(code)); }

                                    private String encode(long value) {
                                        StringBuilder result = new StringBuilder();
                                        do { result.append(ALPHABET[(int) (value % ALPHABET.length)]); value /= ALPHABET.length; }
                                        while (value > 0);
                                        return result.reverse().toString();
                                    }
                                }
                                """, "Implements collision-safe concurrent shortening and resolution")
                ), List.of("A library boundary is sufficient for the greenfield scenario"),
                List.of("Persistence and HTTP delivery are production extensions"));
    }

    private PatchProposal greenfieldTests() {
        return new PatchProposal("Generate greenfield URL-shortener unit tests",
                List.of(create("src/test/java/generated/urlshortener/UrlShortenerTest.java", """
                        package generated.urlshortener;

                        import org.junit.jupiter.api.Test;
                        import java.net.URI;
                        import static org.assertj.core.api.Assertions.*;

                        class UrlShortenerTest {
                            @Test void createsUniqueResolvableCodes() {
                                UrlShortener service = new UrlShortener();
                                URI first = URI.create("https://example.com/one");
                                URI second = URI.create("https://example.com/two");
                                String firstCode = service.shorten(first);
                                String secondCode = service.shorten(second);
                                assertThat(firstCode).isNotEqualTo(secondCode);
                                assertThat(service.resolve(firstCode)).contains(first);
                                assertThat(service.resolve(secondCode)).contains(second);
                            }
                            @Test void rejectsUnsafeSchemes() {
                                assertThatThrownBy(() -> new UrlShortener().shorten(URI.create("file:///secret")))
                                        .isInstanceOf(IllegalArgumentException.class);
                            }
                        }
                        """, "Covers uniqueness, resolution and URL policy")),
                List.of(), List.of());
    }

    private PatchProposal analyticsImplementation() {
        String base = "src/main/java/com/santhosh/fixture/urlshortener/analytics/";
        return new PatchProposal("Add total and daily UTC redirect analytics",
                List.of(
                        create(base + "RedirectAnalyticsService.java", """
                                package com.santhosh.fixture.urlshortener.analytics;

                                import org.springframework.stereotype.Service;
                                import java.time.*;
                                import java.util.Map;
                                import java.util.TreeMap;
                                import java.util.concurrent.ConcurrentHashMap;
                                import java.util.concurrent.atomic.LongAdder;

                                @Service
                                public class RedirectAnalyticsService {
                                    private final Clock clock;
                                    private final ConcurrentHashMap<String, LongAdder> totals = new ConcurrentHashMap<>();
                                    private final ConcurrentHashMap<String, ConcurrentHashMap<LocalDate, LongAdder>> daily = new ConcurrentHashMap<>();
                                    public RedirectAnalyticsService() { this(Clock.systemUTC()); }
                                    RedirectAnalyticsService(Clock clock) { this.clock = clock; }
                                    public void record(String code) {
                                        LocalDate day = LocalDate.now(clock);
                                        totals.computeIfAbsent(code, ignored -> new LongAdder()).increment();
                                        daily.computeIfAbsent(code, ignored -> new ConcurrentHashMap<>())
                                                .computeIfAbsent(day, ignored -> new LongAdder()).increment();
                                    }
                                    public Snapshot snapshot(String code) {
                                        long total = totals.getOrDefault(code, new LongAdder()).sum();
                                        Map<LocalDate, Long> days = new TreeMap<>();
                                        daily.getOrDefault(code, new ConcurrentHashMap<>())
                                                .forEach((day, count) -> days.put(day, count.sum()));
                                        return new Snapshot(code, total, Map.copyOf(days));
                                    }
                                    public record Snapshot(String code, long totalRedirects, Map<LocalDate, Long> dailyUtcRedirects) { }
                                }
                                """, "Maintains thread-safe per-code total and UTC daily counters"),
                        create(base + "RedirectAnalyticsController.java", """
                                package com.santhosh.fixture.urlshortener.analytics;

                                import org.springframework.web.bind.annotation.*;

                                @RestController
                                @RequestMapping("/api/v1/urls/{code}/analytics")
                                public class RedirectAnalyticsController {
                                    private final RedirectAnalyticsService service;
                                    public RedirectAnalyticsController(RedirectAnalyticsService service) { this.service = service; }
                                    @GetMapping
                                    public RedirectAnalyticsService.Snapshot get(@PathVariable String code) {
                                        return service.snapshot(code);
                                    }
                                }
                                """, "Exposes a read-only analytics API"),
                        create(base + "RedirectAnalyticsInterceptor.java", """
                                package com.santhosh.fixture.urlshortener.analytics;

                                import jakarta.servlet.http.*;
                                import org.springframework.http.HttpStatus;
                                import org.springframework.stereotype.Component;
                                import org.springframework.web.servlet.HandlerInterceptor;

                                @Component
                                public class RedirectAnalyticsInterceptor implements HandlerInterceptor {
                                    private final RedirectAnalyticsService service;
                                    public RedirectAnalyticsInterceptor(RedirectAnalyticsService service) { this.service = service; }
                                    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                                                          Object handler, Exception exception) {
                                        String path = request.getRequestURI();
                                        if (exception == null && response.getStatus() == HttpStatus.FOUND.value() &&
                                                path.length() > 1 && !path.startsWith("/api/")) {
                                            service.record(path.substring(1));
                                        }
                                    }
                                }
                                """, "Records only successful redirect responses"),
                        create(base + "AnalyticsWebConfiguration.java", """
                                package com.santhosh.fixture.urlshortener.analytics;

                                import org.springframework.context.annotation.Configuration;
                                import org.springframework.web.servlet.config.annotation.*;

                                @Configuration
                                public class AnalyticsWebConfiguration implements WebMvcConfigurer {
                                    private final RedirectAnalyticsInterceptor interceptor;
                                    public AnalyticsWebConfiguration(RedirectAnalyticsInterceptor interceptor) { this.interceptor = interceptor; }
                                    @Override public void addInterceptors(InterceptorRegistry registry) {
                                        registry.addInterceptor(interceptor).addPathPatterns("/*");
                                    }
                                }
                                """, "Connects analytics to the existing redirect endpoint")
                ), List.of("Redirect responses use HTTP 302"),
                List.of("In-memory counters reset when the fixture restarts"));
    }

    private PatchProposal analyticsTests() {
        return new PatchProposal("Generate redirect analytics unit coverage",
                List.of(create("src/test/java/com/santhosh/fixture/urlshortener/analytics/RedirectAnalyticsServiceTest.java", """
                        package com.santhosh.fixture.urlshortener.analytics;

                        import org.junit.jupiter.api.Test;
                        import java.time.*;
                        import static org.assertj.core.api.Assertions.assertThat;

                        class RedirectAnalyticsServiceTest {
                            @Test void reportsTotalAndDailyUtcCounts() {
                                LocalDate day = LocalDate.of(2026, 8, 22);
                                RedirectAnalyticsService service = new RedirectAnalyticsService(
                                        Clock.fixed(day.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
                                service.record("abc123");
                                service.record("abc123");
                                var snapshot = service.snapshot("abc123");
                                assertThat(snapshot.totalRedirects()).isEqualTo(2);
                                assertThat(snapshot.dailyUtcRedirects()).containsEntry(day, 2L);
                            }
                        }
                        """, "Proves deterministic total and UTC daily aggregation")),
                List.of(), List.of());
    }

    private ProposedFileChange create(String path, String content, String rationale) {
        return new ProposedFileChange(FileChangeType.CREATE, path, null, content, rationale);
    }
}

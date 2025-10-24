package com.trademaster.subscription.security;

import com.trademaster.subscription.common.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Risk Assessment Service Unit Tests
 *
 * MANDATORY: Zero Trust Security Pattern - Rule #6
 * MANDATORY: Risk-based access control with multiple factors
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@DisplayName("Risk Assessment Service Tests")
class RiskAssessmentServiceTest {

    private RiskAssessmentService riskAssessmentService;
    private UUID testUserId;
    private AuthorizationService.AuthzResult authzResult;

    @BeforeEach
    void setUp() {
        riskAssessmentService = new RiskAssessmentService();
        testUserId = UUID.randomUUID();
        authzResult = new AuthorizationService.AuthzResult(
            testUserId,
            "AUTHORIZED",
            System.currentTimeMillis()
        );
    }

    @Nested
    @DisplayName("LOW Risk Assessment Tests")
    class LowRiskAssessmentTests {

        @Test
        @DisplayName("Should assess LOW risk for internal IP with Chrome")
        void shouldAssessLowRiskForInternalIpWithChrome() {
            // Given - Internal IP (192.168), Chrome, recent timestamp, safe path
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RiskResult riskResult = result.getValue();
            assertThat(riskResult.level()).isEqualTo(RiskResult.RiskLevel.LOW);
            assertThat(riskResult.score()).isLessThan(30.0);
        }

        @Test
        @DisplayName("Should assess LOW risk for localhost")
        void shouldAssessLowRiskForLocalhost() {
            // Given - Localhost IP, Chrome, recent timestamp, safe path
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
        }

        @Test
        @DisplayName("Should assess LOW risk for 10.x network with Firefox")
        void shouldAssessLowRiskFor10NetworkWithFirefox() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("10.0.0.1")
                .userAgent("Mozilla/5.0 Firefox/120.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
        }

        @Test
        @DisplayName("Should include LOW risk reason in result")
        void shouldIncludeLowRiskReasonInResult() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().reason()).contains("Low risk");
        }
    }

    @Nested
    @DisplayName("MEDIUM Risk Assessment Tests")
    class MediumRiskAssessmentTests {

        @Test
        @DisplayName("Should assess MEDIUM risk for external IP with Chrome")
        void shouldAssessMediumRiskForExternalIpWithChrome() {
            // Given - External IP (15 points), Chrome (5 points), recent (0 points), cancel path (15 points) = 35 points
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")  // External IP
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions/cancel")  // Cancel path for extra risk
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RiskResult riskResult = result.getValue();
            assertThat(riskResult.level()).isEqualTo(RiskResult.RiskLevel.MEDIUM);
            assertThat(riskResult.score()).isBetween(30.0, 59.9);
        }

        @Test
        @DisplayName("Should assess MEDIUM risk for internal IP with curl")
        void shouldAssessMediumRiskForInternalIpWithCurl() {
            // Given - Internal IP (5 points), curl (20 points), recent (0 points), safe path (5 points) = 30 points
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("curl/7.68.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.MEDIUM);
        }

        @Test
        @DisplayName("Should assess MEDIUM risk for cancel request path")
        void shouldAssessMediumRiskForCancelRequestPath() {
            // Given - Internal IP (5), Chrome (5), recent (0), cancel path (15) = 25... wait, that's LOW
            // Need external IP: External IP (15), Chrome (5), recent (0), cancel path (15) = 35 MEDIUM
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")  // External IP
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions/cancel")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.MEDIUM);
        }

        @Test
        @DisplayName("Should include MEDIUM risk reason in result")
        void shouldIncludeMediumRiskReasonInResult() {
            // Given - Same as above test: External IP + Chrome + cancel path = MEDIUM
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions/cancel")  // Cancel path for MEDIUM risk
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().reason()).contains("Medium risk");
        }
    }

    @Nested
    @DisplayName("HIGH Risk Assessment Tests")
    class HighRiskAssessmentTests {

        @Test
        @DisplayName("Should assess HIGH risk for external IP with bot user agent")
        void shouldAssessHighRiskForExternalIpWithBot() {
            // Given - External IP (15), bot (25), recent (0), delete path (20) = 60 HIGH
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent("Googlebot/2.1")
                .requestPath("/api/subscriptions/delete")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RiskResult riskResult = result.getValue();
            assertThat(riskResult.level()).isEqualTo(RiskResult.RiskLevel.HIGH);
            assertThat(riskResult.score()).isBetween(60.0, 84.9);
        }

        @Test
        @DisplayName("Should assess HIGH risk for old timestamp")
        void shouldAssessHighRiskForOldTimestamp() {
            // Given - External IP (15), Chrome (5), old timestamp (15), delete path (20) = 55... MEDIUM
            // Need to push over 60: External IP (15), bot (25), old timestamp (15), delete path (20) = 75 HIGH
            long oldTimestamp = System.currentTimeMillis() - 600000; // 10 minutes old
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent("Googlebot/2.1")
                .requestPath("/api/subscriptions/delete")
                .timestamp(oldTimestamp)
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.HIGH);
        }

        @Test
        @DisplayName("Should assess HIGH risk for missing user agent")
        void shouldAssessHighRiskForMissingUserAgent() {
            // Given - External IP (15), missing user agent (30), recent (0), delete path (20) = 65 HIGH
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent(null)
                .requestPath("/api/subscriptions/delete")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.HIGH);
        }

        @Test
        @DisplayName("Should include HIGH risk reason in result")
        void shouldIncludeHighRiskReasonInResult() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent("Googlebot/2.1")
                .requestPath("/api/subscriptions/delete")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().reason()).contains("High risk");
        }
    }

    @Nested
    @DisplayName("CRITICAL Risk Assessment Tests")
    class CriticalRiskAssessmentTests {

        @Test
        @DisplayName("Should assess CRITICAL risk for admin path with bot")
        void shouldAssessCriticalRiskForAdminPathWithBot() {
            // Given - External IP (15), bot (25), old timestamp (15), admin path (25) = 80... HIGH
            // Need more: External IP (15), missing user agent (30), old (15), admin (25) = 85 CRITICAL
            long oldTimestamp = System.currentTimeMillis() - 600000;
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent(null)
                .requestPath("/api/admin")
                .timestamp(oldTimestamp)
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RiskResult riskResult = result.getValue();
            assertThat(riskResult.level()).isEqualTo(RiskResult.RiskLevel.CRITICAL);
            assertThat(riskResult.score()).isGreaterThanOrEqualTo(85.0);
        }

        @Test
        @DisplayName("Should assess CRITICAL risk for multiple high-risk factors")
        void shouldAssessCriticalRiskForMultipleHighRiskFactors() {
            // Given - External IP (15), missing UA (30), old timestamp (15), admin path (25) = 85 CRITICAL
            long oldTimestamp = System.currentTimeMillis() - 600000;
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent(null)  // Missing user agent for maximum risk
                .requestPath("/api/admin")
                .timestamp(oldTimestamp)
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RiskResult riskResult = result.getValue();
            assertThat(riskResult.level()).isEqualTo(RiskResult.RiskLevel.CRITICAL);
            assertThat(riskResult.score()).isGreaterThanOrEqualTo(85.0);
        }

        @Test
        @DisplayName("Should cap risk score at 100")
        void shouldCapRiskScoreAt100() {
            // Given - Maximum risk factors
            long oldTimestamp = System.currentTimeMillis() - 600000;
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent(null)
                .requestPath("/api/admin/delete")
                .timestamp(oldTimestamp)
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().score()).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("Should include CRITICAL risk reason in result")
        void shouldIncludeCriticalRiskReasonInResult() {
            // Given
            long oldTimestamp = System.currentTimeMillis() - 600000;
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("203.0.113.1")
                .userAgent(null)
                .requestPath("/api/admin")
                .timestamp(oldTimestamp)
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().reason()).contains("Critical risk");
        }
    }

    @Nested
    @DisplayName("Risk Score Component Tests")
    class RiskScoreComponentTests {

        @Test
        @DisplayName("Should assess internal IP risk correctly")
        void shouldAssessInternalIpRiskCorrectly() {
            // Test 192.168.x.x network
            SecurityContext context192 = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            // Test 10.x.x.x network
            SecurityContext context10 = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("10.0.0.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            Result<RiskResult, String> result192 = riskAssessmentService.assessRisk(authzResult, context192);
            Result<RiskResult, String> result10 = riskAssessmentService.assessRisk(authzResult, context10);

            // Both should be LOW risk
            assertThat(result192.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
            assertThat(result10.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
        }

        @Test
        @DisplayName("Should assess external IP risk correctly")
        void shouldAssessExternalIpRiskCorrectly() {
            // Given - External IP should contribute higher risk
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("8.8.8.8")  // Google DNS - external
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Should be higher risk than internal IP
            assertThat(result.getValue().score()).isGreaterThan(15.0);
        }

        @Test
        @DisplayName("Should assess Chrome user agent as low risk")
        void shouldAssessChromeUserAgentAsLowRisk() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
        }

        @Test
        @DisplayName("Should assess Firefox user agent as low risk")
        void shouldAssessFirefoxUserAgentAsLowRisk() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
        }

        @Test
        @DisplayName("Should assess Safari user agent as low risk")
        void shouldAssessSafariUserAgentAsLowRisk() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
        }

        @Test
        @DisplayName("Should assess recent timestamp as low risk")
        void shouldAssessRecentTimestampAsLowRisk() {
            // Given - Current timestamp
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
        }

        @Test
        @DisplayName("Should assess safe request path as low risk")
        void shouldAssessSafeRequestPathAsLowRisk() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions/list")
                .timestamp(System.currentTimeMillis())
                .build();

            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            assertThat(result.getValue().level()).isEqualTo(RiskResult.RiskLevel.LOW);
        }
    }

    @Nested
    @DisplayName("Result Pattern Tests")
    class ResultPatternTests {

        @Test
        @DisplayName("Should return Result.success for valid assessment")
        void shouldReturnResultSuccessForValidAssessment() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailure()).isFalse();
        }

        @Test
        @DisplayName("Should include all RiskResult fields")
        void shouldIncludeAllRiskResultFields() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 Chrome/120.0.0.0")
                .requestPath("/api/subscriptions")
                .timestamp(System.currentTimeMillis())
                .build();

            // When
            Result<RiskResult, String> result = riskAssessmentService.assessRisk(authzResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RiskResult riskResult = result.getValue();
            assertThat(riskResult.level()).isNotNull();
            assertThat(riskResult.score()).isGreaterThanOrEqualTo(0.0);
            assertThat(riskResult.reason()).isNotEmpty();
        }
    }
}

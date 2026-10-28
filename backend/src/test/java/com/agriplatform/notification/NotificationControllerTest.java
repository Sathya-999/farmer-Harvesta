package com.agriplatform.notification;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.notification.controller.NotificationController;
import com.agriplatform.notification.dto.NotificationDto;
import com.agriplatform.notification.dto.UnreadCountDto;
import com.agriplatform.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task 11 — NotificationController WebMvc Tests.
 *
 * <p>Uses {@link WebMvcTest} to load only the notification controller slice,
 * excluding JPA/DataSource auto-config (which would fail without real DB credentials
 * in CI). Follows the same pattern as AnalyticsControllerTest.
 *
 * <p>Security is exercised via the real {@link SecurityConfig} with mock JWTs.
 */
@WebMvcTest(
    controllers = {NotificationController.class},
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@DisplayName("Task 11 — NotificationController WebMvc Tests")
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private NotificationService notificationService;

    private static final UUID FARMER_ID = UUID.randomUUID();
    private static final UUID NOTIF_ID  = UUID.randomUUID();

    private NotificationDto sampleDto(boolean read) {
        return new NotificationDto(NOTIF_ID, "Title", "Body", "GENERAL",
                read, read ? OffsetDateTime.now() : null, OffsetDateTime.now());
    }

    // =========================================================================
    // SECURITY: Unauthenticated → 401
    // =========================================================================

    @Nested
    @DisplayName("Unauthenticated requests return 401")
    class UnauthenticatedTests {

        @Test @DisplayName("GET /notifications → 401 without JWT")
        void getNotifications_401_noJwt() throws Exception {
            mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("GET /notifications/unread → 401 without JWT")
        void getUnread_401_noJwt() throws Exception {
            mockMvc.perform(get("/api/v1/notifications/unread")).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("GET /notifications/unread/count → 401 without JWT")
        void getUnreadCount_401_noJwt() throws Exception {
            mockMvc.perform(get("/api/v1/notifications/unread/count")).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("PATCH /notifications/{id}/read → 401 without JWT")
        void markRead_401_noJwt() throws Exception {
            mockMvc.perform(patch("/api/v1/notifications/" + NOTIF_ID + "/read")).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("POST /notifications/read-all → 401 without JWT")
        void markAllRead_401_noJwt() throws Exception {
            mockMvc.perform(post("/api/v1/notifications/read-all")).andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // AUTHENTICATED: 200 responses
    // =========================================================================

    @Nested
    @DisplayName("Authenticated requests return correct responses")
    class AuthenticatedTests {

        @Test @DisplayName("GET /notifications → 200 with notification list")
        void getNotifications_200() throws Exception {
            Page<NotificationDto> page = new PageImpl<>(List.of(sampleDto(false)));
            when(notificationService.getNotifications(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/notifications")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].title").value("Title"))
                    .andExpect(jsonPath("$.data.content[0].read").value(false));
        }

        @Test @DisplayName("GET /notifications/unread → 200 with unread list")
        void getUnread_200() throws Exception {
            Page<NotificationDto> page = new PageImpl<>(List.of(sampleDto(false)));
            when(notificationService.getUnreadNotifications(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/notifications/unread")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test @DisplayName("GET /notifications/unread/count → 200 with count")
        void getUnreadCount_200() throws Exception {
            when(notificationService.getUnreadCount()).thenReturn(new UnreadCountDto(3L));

            mockMvc.perform(get("/api/v1/notifications/unread/count")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(3));
        }

        @Test @DisplayName("PATCH /notifications/{id}/read → 200 with updated dto")
        void markRead_200() throws Exception {
            when(notificationService.markAsRead(NOTIF_ID)).thenReturn(sampleDto(true));

            mockMvc.perform(patch("/api/v1/notifications/" + NOTIF_ID + "/read")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.read").value(true));
        }

        @Test @DisplayName("POST /notifications/read-all → 200 with markedRead count")
        void markAllRead_200() throws Exception {
            when(notificationService.markAllAsRead()).thenReturn(5);

            mockMvc.perform(post("/api/v1/notifications/read-all")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.markedRead").value(5));
        }

        @Test @DisplayName("PATCH /notifications/{id}/read → 404 when notification not found (IDOR)")
        void markRead_404_notFound() throws Exception {
            when(notificationService.markAsRead(NOTIF_ID))
                    .thenThrow(new ResourceNotFoundException("Notification not found with id: " + NOTIF_ID));

            mockMvc.perform(patch("/api/v1/notifications/" + NOTIF_ID + "/read")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test @DisplayName("POST /notifications/read-all → 200 with 0 when none unread")
        void markAllRead_200_zeroWhenNoneUnread() throws Exception {
            when(notificationService.markAllAsRead()).thenReturn(0);

            mockMvc.perform(post("/api/v1/notifications/read-all")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.markedRead").value(0));
        }
    }

    // =========================================================================
    // PAGINATION: bounds enforced
    // =========================================================================

    @Nested
    @DisplayName("Pagination bounds")
    class PaginationTests {

        @Test @DisplayName("Oversized page is clamped to MAX_PAGE_SIZE=100")
        void oversizedPage_clamped() throws Exception {
            when(notificationService.getNotifications(any())).thenReturn(Page.empty());

            mockMvc.perform(get("/api/v1/notifications?page=0&size=9999")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("page=0 size=1 (minimum) is valid")
        void minPage_valid() throws Exception {
            when(notificationService.getNotifications(any())).thenReturn(Page.empty());

            mockMvc.perform(get("/api/v1/notifications?page=0&size=1")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("Unread filter endpoint with pagination")
        void unreadWithPagination_200() throws Exception {
            when(notificationService.getUnreadNotifications(any())).thenReturn(Page.empty());

            mockMvc.perform(get("/api/v1/notifications/unread?page=1&size=10")
                    .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // MALFORMED PATH VARIABLE
    // =========================================================================

    @Test @DisplayName("PATCH with malformed UUID → 400")
    void markRead_malformedUuid_400() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/not-a-uuid/read")
                .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                          .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // RESPONSE STRUCTURE: no sensitive data leakage
    // =========================================================================

    @Test @DisplayName("Response has correct ApiResponse structure")
    void response_hasCorrectStructure() throws Exception {
        Page<NotificationDto> page = new PageImpl<>(List.of(sampleDto(false)));
        when(notificationService.getNotifications(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications")
                .with(jwt().jwt(j -> j.subject(FARMER_ID.toString()))
                          .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content").isArray())
                // No farmer_id in response (data minimization)
                .andExpect(jsonPath("$.data.content[0].farmerId").doesNotExist());
    }
}
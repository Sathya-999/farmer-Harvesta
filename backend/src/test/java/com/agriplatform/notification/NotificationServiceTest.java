package com.agriplatform.notification;

import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.notification.dto.CreateNotificationRequest;
import com.agriplatform.notification.dto.NotificationDto;
import com.agriplatform.notification.dto.UnreadCountDto;
import com.agriplatform.notification.entity.Notification;
import com.agriplatform.notification.repository.NotificationRepository;
import com.agriplatform.notification.service.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Task 11 ? NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private FarmerProfileRepository farmerProfileRepository;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private NotificationServiceImpl service;

    private UUID farmerId;
    private FarmerProfile activeFarmer;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        farmerId = UUID.randomUUID();
        activeFarmer = mock(FarmerProfile.class);
        when(activeFarmer.getProfileStatus()).thenReturn(FarmerProfile.STATUS_ACTIVE);
        pageable = PageRequest.of(0, 20);
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Returns page of notifications for active farmer")
        void returnsPage_forActiveFarmer() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            Notification n = new Notification(farmerId, "Hello", "World", "GENERAL");
            when(notificationRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId, pageable))
                    .thenReturn(new PageImpl<>(List.of(n)));

            Page<NotificationDto> result = service.getNotifications(pageable);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("Hello");
            assertThat(result.getContent().get(0).read()).isFalse();
        }

        @Test
        @DisplayName("Returns empty page when no notifications")
        void returnsEmpty_whenNoNotifications() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            when(notificationRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId, pageable))
                    .thenReturn(Page.empty());

            Page<NotificationDto> result = service.getNotifications(pageable);
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Throws 404 for deleted farmer")
        void throws404_forDeletedFarmer() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            FarmerProfile deleted = mock(FarmerProfile.class);
            when(deleted.getProfileStatus()).thenReturn(FarmerProfile.STATUS_DELETED);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> service.getNotifications(pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Suspended farmer can still read notifications")
        void suspendedFarmer_canRead() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            FarmerProfile suspended = mock(FarmerProfile.class);
            when(suspended.getProfileStatus()).thenReturn(FarmerProfile.STATUS_SUSPENDED);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(suspended));
            when(notificationRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId, pageable))
                    .thenReturn(Page.empty());

            Page<NotificationDto> result = service.getNotifications(pageable);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("getUnreadNotifications")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Returns only unread notifications")
        void returnsOnlyUnread() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            Notification unread = new Notification(farmerId, "Unread", "Msg", "GENERAL");
            when(notificationRepository.findByFarmerIdAndReadAtIsNullOrderByCreatedAtDesc(farmerId, pageable))
                    .thenReturn(new PageImpl<>(List.of(unread)));

            Page<NotificationDto> result = service.getUnreadNotifications(pageable);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).read()).isFalse();
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Returns correct unread count")
        void returnsCorrectCount() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            when(notificationRepository.countByFarmerIdAndReadAtIsNull(farmerId)).thenReturn(7L);

            UnreadCountDto result = service.getUnreadCount();
            assertThat(result.unreadCount()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Returns zero when all read")
        void returnsZero_whenAllRead() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            when(notificationRepository.countByFarmerIdAndReadAtIsNull(farmerId)).thenReturn(0L);

            UnreadCountDto result = service.getUnreadCount();
            assertThat(result.unreadCount()).isZero();
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("Marks unread notification as read, returns updated DTO")
        void marksUnread_asRead() {
            UUID notifId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));

            // First call: unread entity; second call (re-fetch after @Modifying): same mock returns the entity.
            // The @Modifying(clearAutomatically=true) ensures JPA cache is cleared so re-fetch hits DB.
            // In unit tests the mock is not affected by cache clearing, but the behaviour is documented.
            Notification unread = new Notification(farmerId, "T", "B", "GENERAL");
            when(notificationRepository.findByIdAndFarmerId(notifId, farmerId))
                    .thenReturn(Optional.of(unread))
                    .thenReturn(Optional.of(unread));
            when(notificationRepository.markAsRead(eq(notifId), eq(farmerId), any())).thenReturn(1);

            NotificationDto result = service.markAsRead(notifId);
            assertThat(result).isNotNull();
            // The DB update is called exactly once
            verify(notificationRepository).markAsRead(eq(notifId), eq(farmerId), any(OffsetDateTime.class));
            // Re-fetch is called after the update (the second findByIdAndFarmerId call)
            verify(notificationRepository, times(2)).findByIdAndFarmerId(notifId, farmerId);
        }

        @Test
        @DisplayName("Already-read notification: markAsRead is idempotent (no DB update)")
        void alreadyRead_isIdempotent() {
            UUID notifId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));

            // Simulate already-read notification by using a mock
            Notification readNotif = mock(Notification.class);
            when(readNotif.isRead()).thenReturn(true);
            when(readNotif.getId()).thenReturn(notifId);
            // getFarmerId() not called by toDto(), so not stubbed
            when(readNotif.getTitle()).thenReturn("T");
            when(readNotif.getBody()).thenReturn("B");
            when(readNotif.getType()).thenReturn("GENERAL");
            when(readNotif.getReadAt()).thenReturn(OffsetDateTime.now());
            when(readNotif.getCreatedAt()).thenReturn(OffsetDateTime.now());
            when(notificationRepository.findByIdAndFarmerId(notifId, farmerId))
                    .thenReturn(Optional.of(readNotif));

            NotificationDto result = service.markAsRead(notifId);
            assertThat(result).isNotNull();
            assertThat(result.read()).isTrue();
            // DB update should NOT be called for already-read notification
            verify(notificationRepository, never()).markAsRead(any(), any(), any());
            // Re-fetch is NOT called for already-read (service short-circuits at isRead() check)
            verify(notificationRepository, times(1)).findByIdAndFarmerId(notifId, farmerId);
        }

        @Test
        @DisplayName("Throws 404 for notification not owned by farmer (IDOR)")
        void throws404_forForeignNotification() {
            UUID notifId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            when(notificationRepository.findByIdAndFarmerId(notifId, farmerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsRead(notifId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(notifId.toString());
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Returns count of notifications marked read")
        void returnsCount() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            when(notificationRepository.markAllAsRead(eq(farmerId), any())).thenReturn(5);

            int result = service.markAllAsRead();
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("Returns 0 when no unread notifications")
        void returnsZero_whenNoneUnread() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            when(notificationRepository.markAllAsRead(eq(farmerId), any())).thenReturn(0);

            int result = service.markAllAsRead();
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("markAllAsRead only updates authenticated farmer's notifications")
        void onlyUpdatesOwnFarmer() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            when(notificationRepository.markAllAsRead(eq(farmerId), any())).thenReturn(3);

            service.markAllAsRead();
            // Verify called with the authenticated farmerId, not any other
            verify(notificationRepository).markAllAsRead(eq(farmerId), any(OffsetDateTime.class));
        }
    }

    @Nested
    @DisplayName("createForFarmer")
    class CreateForFarmerTests {

        @Test
        @DisplayName("Creates notification for active farmer")
        void creates_forActiveFarmer() {
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
            Notification saved = new Notification(farmerId, "Alert", "Your crop is ready", "CULTIVATION");
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            CreateNotificationRequest req = new CreateNotificationRequest("Alert", "Your crop is ready", "CULTIVATION");
            NotificationDto result = service.createForFarmer(farmerId, req);
            assertThat(result.title()).isEqualTo("Alert");
            assertThat(result.type()).isEqualTo("CULTIVATION");
        }

        @Test
        @DisplayName("Throws 404 for SUSPENDED farmer (cannot receive new notifications)")
        void throws404_forSuspendedFarmer() {
            FarmerProfile suspended = mock(FarmerProfile.class);
            when(suspended.getProfileStatus()).thenReturn(FarmerProfile.STATUS_SUSPENDED);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(suspended));

            CreateNotificationRequest req = new CreateNotificationRequest("T", "B", null);
            assertThatThrownBy(() -> service.createForFarmer(farmerId, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throws 404 for DELETED farmer")
        void throws404_forDeletedFarmer() {
            FarmerProfile deleted = mock(FarmerProfile.class);
            when(deleted.getProfileStatus()).thenReturn(FarmerProfile.STATUS_DELETED);
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(deleted));

            CreateNotificationRequest req = new CreateNotificationRequest("T", "B", null);
            assertThatThrownBy(() -> service.createForFarmer(farmerId, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
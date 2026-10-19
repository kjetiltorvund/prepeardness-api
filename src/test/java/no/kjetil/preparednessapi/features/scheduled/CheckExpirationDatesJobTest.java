package no.kjetil.preparednessapi.features.scheduled;

import no.kjetil.preparednessapi.features.appuser.service.AppUserService;
import no.kjetil.preparednessapi.features.articleitem.service.ItemService;
import no.kjetil.preparednessapi.features.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class CheckExpirationDatesJobTest {

    @Mock
    private ItemService itemService;

    @Mock
    private EmailService emailService;

    @Mock
    private AppUserService appUserService;

    @BeforeEach
    public void setup() {
        itemService = mock(ItemService.class);
        emailService = mock(EmailService.class);
        appUserService = mock(AppUserService.class);
    }

    @Test
    public void shouldCallSendEmailWhenArticleIsNearingItsExpirationDate() {
        // Arrange
        when(itemService.findAllByDatePassedExpirationDate(any())).thenReturn(
                ArticleItemTestData.randomExpiredList(10)
        );

        CheckExpirationDatesJob checkExpirationDatesJob = new CheckExpirationDatesJob(itemService, emailService, appUserService);

        // Act
        checkExpirationDatesJob.checkIfAnythingIsExpired();

        // Assert
        Mockito.verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }
}
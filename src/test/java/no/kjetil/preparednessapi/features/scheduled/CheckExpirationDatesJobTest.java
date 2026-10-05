package no.kjetil.preparednessapi.features.scheduled;

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

    @BeforeEach
    public void setup() {
        itemService = mock(ItemService.class);
        emailService = mock(EmailService.class);
    }

    @Test
    public void shouldCallSendEmailWhenArticleIsNearingItsExpirationDate() {
        // Arrange
        when(itemService.findAllByDatePassedExpirationDate(any())).thenReturn(
                ArticleItemTestData.randomExpiredList(10)
        );

        CheckExpirationDatesJob checkExpirationDatesJob = new CheckExpirationDatesJob(itemService, emailService);

        // Act
        checkExpirationDatesJob.checkIfAnythingIsExpired();

        // Assert
        Mockito.verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }
}
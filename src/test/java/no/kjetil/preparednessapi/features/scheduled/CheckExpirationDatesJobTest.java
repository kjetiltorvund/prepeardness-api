package no.kjetil.preparednessapi.features.scheduled;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.kjetil.preparednessapi.features.articleitem.service.ArticleItemService;
import no.kjetil.preparednessapi.features.email.service.EmailService;
import static org.mockito.Mockito.*;

class CheckExpirationDatesJobTest {

    @Mock
    private ArticleItemService articleItemService;

    @Mock
    private EmailService emailService;

    @BeforeEach
    public void setup() {
        articleItemService = mock(ArticleItemService.class);
        emailService = mock(EmailService.class);
    }

    @Test
    public void shouldCallSendEmailWhenArticleIsNearingItsExpirationDate() {
        // Arrange
        when(articleItemService.findAllByDatePassedExpirationDate(any())).thenReturn(
                ArticleItemTestData.randomExpiredList(10)
        );

        CheckExpirationDatesJob checkExpirationDatesJob = new CheckExpirationDatesJob(articleItemService, emailService);

        // Act
        checkExpirationDatesJob.checkIfAnythingIsExpired();

        // Assert
        Mockito.verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }
}
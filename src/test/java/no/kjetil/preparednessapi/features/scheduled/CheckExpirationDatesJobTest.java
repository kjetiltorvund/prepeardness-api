package no.kjetil.prepeardnessapi.features.scheduled;

import no.kjetil.prepeardnessapi.features.articleitem.repositories.ArticleItemRepository;
import no.kjetil.prepeardnessapi.features.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class CheckExpirationDatesJobTest {

    @Mock
    private ArticleItemRepository articleItemRepository;

    @Mock
    private EmailService emailService;

    @BeforeEach
    public void setup() {
        articleItemRepository = mock(ArticleItemRepository.class);
        emailService = mock(EmailService.class);
    }

    @Test
    public void shouldCallSendEmailWhenArticleIsNearingItsExpirationDate() {
        // Arrange
        when(articleItemRepository.findAllByDatePassedExpirationDate(any())).thenReturn(
                ArticleItemTestData.randomExpiredList(10)
        );

        CheckExpirationDatesJob checkExpirationDatesJob = new CheckExpirationDatesJob(articleItemRepository, emailService);

        // Act
        checkExpirationDatesJob.checkIfAnythingIsExpired();

        // Assert
        Mockito.verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }
}
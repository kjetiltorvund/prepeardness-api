package no.kjetil.preparednessapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class VerifyMockito {

    @Test
    public void verifyMockito() {
        List mockedLists = mock(List.class);

        mockedLists.add("one");
        mockedLists.clear();

        verify(mockedLists).add("one");
        verify(mockedLists).clear();
    }
}

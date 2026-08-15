package UrlShortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import UrlShortener.exception.InvalidUrlException;
import UrlShortener.exception.ShortCodeNotFoundException;
import UrlShortener.model.Url;
import UrlShortener.repository.UrlRepository;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private ConversionService conversionService;

    // -------------------------------------------------------------------------
    // convert() tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("convert: invalid URL throws InvalidUrlException")
    void convert_withInvalidUrl_throwsInvalidUrlException() {
        assertThatThrownBy(() -> conversionService.convert("not-a-valid-url"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("not-a-valid-url");
    }

    @Test
    @DisplayName("convert: URL without www prefix is rejected")
    void convert_withoutWwwPrefix_throwsInvalidUrlException() {
        assertThatThrownBy(() -> conversionService.convert("https://example.com"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    @DisplayName("convert: new valid URL is shortened and saved")
    void convert_withNewValidUrl_returnsShortUrl() {
        String inputUrl = "https://www.example.com/some/path";
        String sanitizedUrl = "www.example.com/some/path";

        // Simulate DB returning entity with auto-generated id = 1
        Url savedUrl = new Url(sanitizedUrl);
        ReflectionTestUtils.setField(savedUrl, "id", 1L);

        when(urlRepository.findByLongUrl(sanitizedUrl)).thenReturn(Optional.empty());
        when(urlRepository.saveAndFlush(any(Url.class))).thenReturn(savedUrl);

        String result = conversionService.convert(inputUrl);

        assertThat(result).startsWith("https://www.urlShortener.com/");
        // saveAndFlush called twice: once to get ID, once to persist short code
        verify(urlRepository, times(2)).saveAndFlush(any(Url.class));
    }

    @Test
    @DisplayName("convert: http:// URL is also accepted and sanitized")
    void convert_withHttpUrl_returnsShortUrl() {
        String inputUrl = "http://www.example.com/page";
        String sanitizedUrl = "www.example.com/page";

        Url savedUrl = new Url(sanitizedUrl);
        ReflectionTestUtils.setField(savedUrl, "id", 2L);

        when(urlRepository.findByLongUrl(sanitizedUrl)).thenReturn(Optional.empty());
        when(urlRepository.saveAndFlush(any(Url.class))).thenReturn(savedUrl);

        String result = conversionService.convert(inputUrl);

        assertThat(result).startsWith("https://www.urlShortener.com/");
    }

    @Test
    @DisplayName("convert: trailing slash is stripped before storing")
    void convert_withTrailingSlash_stripsSlashBeforeSaving() {
        String inputUrl = "https://www.example.com/";
        String sanitizedUrl = "www.example.com";

        Url savedUrl = new Url(sanitizedUrl);
        ReflectionTestUtils.setField(savedUrl, "id", 3L);

        when(urlRepository.findByLongUrl(sanitizedUrl)).thenReturn(Optional.empty());
        when(urlRepository.saveAndFlush(any(Url.class))).thenReturn(savedUrl);

        conversionService.convert(inputUrl);

        // Verify the repository was queried with the sanitized (no trailing slash) URL
        verify(urlRepository).findByLongUrl(sanitizedUrl);
    }

    @Test
    @DisplayName("convert: already shortened URL returns cached short code without re-inserting")
    void convert_withExistingUrl_returnsCachedShortUrl() {
        String inputUrl = "https://www.example.com/page";
        String sanitizedUrl = "www.example.com/page";

        Url existing = new Url(sanitizedUrl);
        existing.setShortCode("abc123");

        when(urlRepository.findByLongUrl(sanitizedUrl)).thenReturn(Optional.of(existing));

        String result = conversionService.convert(inputUrl);

        assertThat(result).isEqualTo("https://www.urlShortener.com/abc123");
        // No DB insert should happen
        verify(urlRepository, never()).saveAndFlush(any(Url.class));
    }

    // -------------------------------------------------------------------------
    // getLongUrl() tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getLongUrl: valid short code returns reconstructed URL")
    void getLongUrl_withValidShortCode_returnsReconstructedUrl() {
        Url url = new Url("www.example.com/path");
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        String result = conversionService.getLongUrl("abc123");

        assertThat(result).isEqualTo("https://www.example.com/path");
    }

    @Test
    @DisplayName("getLongUrl: unknown short code throws ShortCodeNotFoundException")
    void getLongUrl_withUnknownShortCode_throwsShortCodeNotFoundException() {
        when(urlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversionService.getLongUrl("unknown"))
                .isInstanceOf(ShortCodeNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}

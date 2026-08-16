package UrlShortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(conversionService, "domain", "https://www.urlShortener.com/");
        // Dedup disabled by default so existing tests are unaffected
        ReflectionTestUtils.setField(conversionService, "deduplicationEnabled", false);
        ReflectionTestUtils.setField(conversionService, "deduplicationStrategy", "USERNAME");
        ReflectionTestUtils.setField(conversionService, "deduplicationWindowMinutes", 60);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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

        String result = conversionService.getLongUrl("abc123", "127.0.0.1");

        assertThat(result).isEqualTo("https://www.example.com/path");
    }

    @Test
    @DisplayName("getLongUrl: unknown short code throws ShortCodeNotFoundException")
    void getLongUrl_withUnknownShortCode_throwsShortCodeNotFoundException() {
        when(urlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversionService.getLongUrl("unknown", "127.0.0.1"))
                .isInstanceOf(ShortCodeNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("getLongUrl: valid short code increments click count")
    void getLongUrl_withExistingShortCode_incrementsClickCount() {
        Url url = new Url("www.example.com/path");
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        conversionService.getLongUrl("abc123", "127.0.0.1");

        verify(urlRepository).incrementClickCount("abc123");
    }

    @Test
    @DisplayName("getLongUrl: unknown short code does not increment click count")
    void getLongUrl_withUnknownShortCode_doesNotIncrementClickCount() {
        when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversionService.getLongUrl("missing", "127.0.0.1"))
                .isInstanceOf(ShortCodeNotFoundException.class);

        verify(urlRepository, never()).incrementClickCount(anyString());
    }

    // -------------------------------------------------------------------------
    // isDuplicate / deduplication tests (TF02)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("dedup USERNAME: same user same code within window — second click skipped")
    void getLongUrl_withDuplicateUsernameWithinWindow_skipsIncrement() {
        ReflectionTestUtils.setField(conversionService, "deduplicationEnabled", true);
        ReflectionTestUtils.setField(conversionService, "deduplicationWindowMinutes", 60);
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken("alice", null, Collections.emptyList())));

        Url url = new Url("www.example.com");
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        conversionService.getLongUrl("abc123", "127.0.0.1");
        conversionService.getLongUrl("abc123", "127.0.0.1");

        verify(urlRepository, times(1)).incrementClickCount("abc123");
    }

    @Test
    @DisplayName("dedup USERNAME: anonymous user is never deduplicated — both clicks count")
    void getLongUrl_withAnonymousUser_usernameStrategy_alwaysIncrements() {
        ReflectionTestUtils.setField(conversionService, "deduplicationEnabled", true);
        // SecurityContextHolder cleared in @AfterEach — anonymous by default (no context)

        Url url = new Url("www.example.com");
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        conversionService.getLongUrl("abc123", "127.0.0.1");
        conversionService.getLongUrl("abc123", "127.0.0.1");

        verify(urlRepository, times(2)).incrementClickCount("abc123");
    }

    @Test
    @DisplayName("dedup IP: same IP same code within window — second click skipped")
    void getLongUrl_withDuplicateIpWithinWindow_ipStrategy_skipsIncrement() {
        ReflectionTestUtils.setField(conversionService, "deduplicationEnabled", true);
        ReflectionTestUtils.setField(conversionService, "deduplicationStrategy", "IP");
        ReflectionTestUtils.setField(conversionService, "deduplicationWindowMinutes", 60);

        Url url = new Url("www.example.com");
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        conversionService.getLongUrl("abc123", "10.0.0.1");
        conversionService.getLongUrl("abc123", "10.0.0.1");

        verify(urlRepository, times(1)).incrementClickCount("abc123");
    }

    @Test
    @DisplayName("dedup IP: different IPs same code — both clicks count")
    void getLongUrl_withDifferentIps_ipStrategy_bothIncrement() {
        ReflectionTestUtils.setField(conversionService, "deduplicationEnabled", true);
        ReflectionTestUtils.setField(conversionService, "deduplicationStrategy", "IP");

        Url url = new Url("www.example.com");
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        conversionService.getLongUrl("abc123", "10.0.0.1");
        conversionService.getLongUrl("abc123", "10.0.0.2");

        verify(urlRepository, times(2)).incrementClickCount("abc123");
    }

    // -------------------------------------------------------------------------
    // getAllUrls() tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAllUrls: returns all URLs from repository")
    void getAllUrls_returnsAllUrlsFromRepository() {
        Url url1 = new Url("www.example.com");
        url1.setShortCode("abc1");
        Url url2 = new Url("www.google.com");
        url2.setShortCode("abc2");

        when(urlRepository.findAll()).thenReturn(Arrays.asList(url1, url2));

        List<Url> result = conversionService.getAllUrls();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(url1, url2);
        verify(urlRepository).findAll();
    }
}

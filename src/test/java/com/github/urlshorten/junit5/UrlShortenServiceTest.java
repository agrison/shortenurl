package com.github.urlshorten.junit5;

import com.github.urlshorten.IdStrategy;
import com.github.urlshorten.Url;
import com.github.urlshorten.UrlRepository;
import com.github.urlshorten.UrlShortenService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

class UrlShortenServiceTest {

  @Mock
  UrlRepository repository;

  @Captor
  ArgumentCaptor<Url> urlCaptor;

  UrlShortenService service;

  @BeforeEach
  void set_up() {
    MockitoAnnotations.initMocks(this);

    service = new UrlShortenService(repository);
  }

  @AfterEach
  void tear_down() {
    verifyNoMoreInteractions(repository);
  }

  @Nested
  class UrlShortenServiceShortenUrl {

    @BeforeEach
    void set_up() {
      given(repository.save(any(Url.class))).willAnswer(invocation -> {
        var urlEntity = (Url) invocation.getArgument(0);
        urlEntity.setId(123456789L);
        return urlEntity;
      });
    }

    @DisplayName("should shorten url based on...")
    @ParameterizedTest(name = " strategy {0}")
    @EnumSource(IdStrategy.class)
    void should_shortern_url(IdStrategy strategy) {
      // given
      var goodUrl = "https://medium.com/swlh/how-to-build-a-tiny-url-service-that-scales-to-billions-f6fb5ea22e8c";

      // when
      var result = service.shorten(goodUrl, strategy);

      // then
      verify(repository).save(urlCaptor.capture());
      var url = urlCaptor.getValue();
      assertAll(
          () -> assertEquals(goodUrl, url.getOriginalUrl()),
          () -> assertNotNull(url.getId()));

      assertTrue(result.getShortenUrl().contains("medium.com"));
      assertFalse(result.getShortenUrl().contains("swlh/how-to-build-a-tiny-url-service-that-scales-to-billions-f6fb5ea22e8c"));
    }

    @DisplayName("should shorten urls...")
    @ParameterizedTest(name = "shorten {0}")
    @CsvFileSource(resources = "/urls.csv")
    void should_shorten(@AggregateWith(TestedUrlAggregator.class) TestedUrl testedUrl) {
      assertEquals(service.shorten(testedUrl.getOriginUrl(), testedUrl.getStrategy()).getShortenUrl(),
          testedUrl.getTargetUrl());

      verify(repository).save(urlCaptor.capture());
      var url = urlCaptor.getValue();
      assertAll(
          () -> assertEquals(testedUrl.getOriginUrl(), url.getOriginalUrl()),
          () -> assertEquals(testedUrl.getTargetUrl(), url.getShortenUrl()));
    }


    @Test
    void should_failed_to_shorten() {
      // given
      var wrongUrl = "hmerhmeoj(you'tueido";

      // when
      Executable executable = () -> service.shorten(wrongUrl, IdStrategy.BASE_62);

      // then
      assertThrows(IllegalArgumentException.class, executable);

      verify(repository).save(urlCaptor.capture());
      var url = urlCaptor.getValue();
      assertAll(
          () -> assertEquals(wrongUrl, url.getOriginalUrl()),
          () -> assertNotNull(url.getId()));
    }
  }

  @Test
  void should_retrieve() {
    // given
    var url = "https://google.com";
    var entity = new Url();
    given(repository.findByShortenUrl(anyString())).willReturn(Optional.of(entity));

    // when
    var optUrl = service.retrieve(url);

    // then
    assertTrue(optUrl.isPresent());
    assertSame(entity, optUrl.get());

    verify(repository).findByShortenUrl(url);
  }

}
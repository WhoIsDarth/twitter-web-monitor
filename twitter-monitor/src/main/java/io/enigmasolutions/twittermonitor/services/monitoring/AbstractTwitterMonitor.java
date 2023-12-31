package io.enigmasolutions.twittermonitor.services.monitoring;

import static io.enigmasolutions.twittermonitor.utils.TweetGenerator.buildBriefTweet;
import static io.enigmasolutions.twittermonitor.utils.TweetGenerator.generate;

import io.enigmasolutions.broadcastmodels.Alert;
import io.enigmasolutions.broadcastmodels.BriefTweet;
import io.enigmasolutions.broadcastmodels.Media;
import io.enigmasolutions.broadcastmodels.MediaType;
import io.enigmasolutions.broadcastmodels.Recognition;
import io.enigmasolutions.broadcastmodels.Tweet;
import io.enigmasolutions.broadcastmodels.TwitterUser;
import io.enigmasolutions.twittermonitor.db.models.documents.TwitterScraper;
import io.enigmasolutions.twittermonitor.db.repositories.TwitterScraperRepository;
import io.enigmasolutions.twittermonitor.exceptions.MonitorRunningException;
import io.enigmasolutions.twittermonitor.exceptions.NoTargetMatchesException;
import io.enigmasolutions.twittermonitor.models.external.MonitorStatus;
import io.enigmasolutions.twittermonitor.models.monitor.Status;
import io.enigmasolutions.twittermonitor.models.twitter.base.TweetResponse;
import io.enigmasolutions.twittermonitor.services.kafka.KafkaProducer;
import io.enigmasolutions.twittermonitor.services.recognition.ImageRecognitionProcessor;
import io.enigmasolutions.twittermonitor.services.recognition.PlainTextRecognitionProcessor;
import io.enigmasolutions.twittermonitor.services.recognition.RecognitionProcessor;
import io.enigmasolutions.twittermonitor.services.web.TwitterCustomClient;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

public abstract class AbstractTwitterMonitor {

  protected final TwitterScraperRepository twitterScraperRepository;
  protected final TwitterHelperService twitterHelperService;
  protected final ExecutorService processingExecutor = Executors.newCachedThreadPool();
  protected final KafkaProducer kafkaProducer;
  private final Timer timer = new Timer();
  private final ExecutorService mainThreadExecutor = Executors.newSingleThreadExecutor();
  private final int timelineDelay;
  private final Logger log;
  private final List<PlainTextRecognitionProcessor> plainTextRecognitionProcessors;
  private final List<ImageRecognitionProcessor> imageRecognitionProcessors;

  protected List<TwitterCustomClient> failedCustomClients =
      Collections.synchronizedList(new LinkedList<>());
  protected List<TwitterCustomClient> twitterCustomClients;
  private Status status = Status.STOPPED;
  private Integer delay = null;
  private MultiValueMap<String, String> params;

  public AbstractTwitterMonitor(
      int timelineDelay,
      TwitterScraperRepository twitterScraperRepository,
      TwitterHelperService twitterHelperService,
      KafkaProducer kafkaProducer,
      List<PlainTextRecognitionProcessor> plainTextRecognitionProcessors,
      List<ImageRecognitionProcessor> imageRecognitionProcessors,
      Logger log) {
    this.timelineDelay = timelineDelay;
    this.twitterScraperRepository = twitterScraperRepository;
    this.twitterHelperService = twitterHelperService;
    this.kafkaProducer = kafkaProducer;
    this.plainTextRecognitionProcessors = plainTextRecognitionProcessors;
    this.imageRecognitionProcessors = imageRecognitionProcessors;
    this.log = log;
  }

  public Status getStatus() {
    return status;
  }

  public MultiValueMap<String, String> getParams() {
    return params;
  }

  public void start() {
    synchronized (this) {
      if (status != Status.STOPPED) {
        throw new MonitorRunningException();
      }
      status = Status.RUNNING;
      params = generateParams();
      initTwitterCustomClients();
    }

    mainThreadExecutor.execute(this::runMonitor);
  }

  public void stop() {
    if (status == Status.STOPPED) {
      return;
    }

    status = Status.STOPPED;
    failedCustomClients.clear();

    log.info("Monitor has been stopped");
  }

  public MonitorStatus getMonitorStatus() {
    return MonitorStatus.builder().status(status).build();
  }

  protected abstract void executeTwitterMonitoring();

  protected abstract MultiValueMap<String, String> generateParams();

  protected TweetResponse getTweetResponse(
      MultiValueMap<String, String> params,
      String timelinePath,
      TwitterCustomClient twitterCustomClient) {
    TweetResponse tweetResponse = null;

    TweetResponse[] tweetResponseArray =
        twitterCustomClient.getBaseApiTimelineTweets(params, timelinePath).getBody();

    if (tweetResponseArray != null && tweetResponseArray.length > 0) {
      tweetResponse = tweetResponseArray[0];
    }

    return tweetResponse;
  }

  protected void processTweetResponse(TweetResponse tweetResponse) {
    if (tweetResponse == null) {
      return;
    }

    if (isTweetRelevant(tweetResponse)
        && !twitterHelperService.isTweetInCache(tweetResponse.getTweetId())) {
      Tweet tweet = generate(tweetResponse);

      log.info("Received tweet for processing: {}", tweet);

      processingExecutor.execute(() -> tweetProcessing(tweet));
      processingExecutor.execute(() -> recognitionProcessing(tweetResponse, tweet));
    }
  }

  protected void processErrorResponse(
      HttpClientErrorException exception, TwitterCustomClient twitterCustomClient) {
    if (exception.getStatusCode().value() < 500 && exception.getStatusCode().value() != 401) {
      log.error(exception.toString());
    }

    if (exception.getStatusCode().value() == 401) {
      log.error(exception.toString());
    }

    if (exception.getStatusCode().value() >= 400
        && exception.getStatusCode().value() < 500
        && exception.getStatusCode().value() != 404
        && exception.getStatusCode().value() != 401) {

      reshuffleClients(twitterCustomClient);
      processTemporaryError(exception, twitterCustomClient);
      calculateDelay();

      processingExecutor.execute(() -> processAlertTarget(exception, twitterCustomClient));

      if (failedCustomClients.size() > 15) {
        stop();
      }
    }
  }

  private void reshuffleClients(TwitterCustomClient twitterCustomClient) {
    twitterCustomClients.remove(twitterCustomClient);
    failedCustomClients.add(twitterCustomClient);
  }

  private void processTemporaryError(
      HttpClientErrorException exception, TwitterCustomClient twitterCustomClient) {
    Long delay = null;
    final long RATE_LIMIT_DELAY = 60000;
    final long TIMEOUT_DELAY = 10000;

    if (exception.getStatusCode().value() == 429) {
      delay = RATE_LIMIT_DELAY;
    } else if (exception.getStatusCode().value() == 408) {
      delay = TIMEOUT_DELAY;
    }

    if (delay != null) {
      TimerTask timerTask =
              new TimerTask() {
                @Override
                public void run() {
                  restoreFailedClient(twitterCustomClient);
                }
              };

      timer.schedule(timerTask, delay);
    }
  }

  private void calculateDelay() {
    delay = timelineDelay / twitterCustomClients.size();
  }

  public void restoreFailedClient(TwitterCustomClient twitterCustomClient) {
    if (failedCustomClients.isEmpty()) {
      throw new NoTargetMatchesException();
    }

    if (twitterCustomClient.getTwitterScraper().getCredentials() == null) {
      twitterCustomClient =
          getFullFailedClient(
              twitterCustomClient.getTwitterScraper().getTwitterUser().getTwitterId());
    }

    failedCustomClients.remove(twitterCustomClient);
    twitterCustomClients.add(twitterCustomClient);
    log.info(
        "Scraper " + twitterCustomClient.getTwitterScraper().getId() + " successfully restored!");
  }

  private TwitterCustomClient getFullFailedClient(String twitterId) {
    TwitterCustomClient fullCustomClient = null;

    for (TwitterCustomClient failedCustomClient : failedCustomClients) {
      if (failedCustomClient
          .getTwitterScraper()
          .getTwitterUser()
          .getTwitterId()
          .equals(twitterId)) {
        fullCustomClient = failedCustomClient;
      }
    }

    if (fullCustomClient == null) {
      throw new NoTargetMatchesException();
    }

    return fullCustomClient;
  }

  private void processAlertTarget(
      HttpClientErrorException exception, TwitterCustomClient twitterCustomClient) {
    Alert alert =
        Alert.builder()
            .failedMonitorId(
                twitterCustomClient.getTwitterScraper().getTwitterUser().getTwitterId())
            .failedMonitorsCount(failedCustomClients.size())
            .validMonitorsCount(twitterCustomClients.size())
            .reason(exception.getMessage())
            .build();

    kafkaProducer.sendAlertBroadcast(alert);
  }

  private void runMonitor() {
    if (twitterCustomClients.isEmpty()) {
      return;
    }

    calculateDelay();

    log.info("Monitor has been started");

    while (status == Status.RUNNING) {
      try {
        Thread.sleep(delay);
        processingExecutor.execute(this::executeTwitterMonitoring);
      } catch (Exception exception) {
        log.error("Unknown exception", exception);
      }
    }
  }

  private void initTwitterCustomClients() {
    List<TwitterScraper> scrapers = twitterScraperRepository.findAll();

    prepareClients(scrapers);
  }

  protected abstract void prepareClients(List<TwitterScraper> scrapers);

  private Boolean isTweetRelevant(TweetResponse tweetResponse) {
    return new Date().getTime() - Date.parse(tweetResponse.getCreatedAt()) <= 25000;
  }

  protected synchronized TwitterCustomClient refreshClient() {
    TwitterCustomClient client = twitterCustomClients.remove(0);
    twitterCustomClients.add(client);

    return client;
  }

  private void tweetProcessing(Tweet tweet) {
    processingExecutor.execute(() -> processCommonTarget(tweet));
    processingExecutor.execute(() -> processLiveReleaseTarget(tweet));
  }

  private void recognitionProcessing(TweetResponse tweetResponse, Tweet tweet) {
    BriefTweet briefTweet = buildBriefTweet(tweetResponse);

    processingExecutor.execute(
        () -> {
          List<String> images =
              tweet.getMedia().stream()
                  .filter(media -> media.getType().equals(MediaType.PHOTO))
                  .map(Media::getStatical)
                  .collect(Collectors.toList());
          processImageRecognition(tweet, briefTweet, images);
        });
    processingExecutor.execute(
        () -> processPlainTextRecognition(tweet, briefTweet, null, tweet.getDetectedUrls()));
  }

  private void processPlainTextRecognition(
      Tweet tweet, BriefTweet briefTweet, BriefTweet nestedBriefTweet, List<String> plainTextUrls) {
    plainTextRecognitionProcessors.forEach(
        recognitionProcessor ->
            processingExecutor.execute(
                () ->
                    plainTextUrls.forEach(
                        url ->
                            processingExecutor.execute(
                                () ->
                                    recognitionProcessingWrapper(
                                        recognitionProcessor,
                                        tweet,
                                        url,
                                        briefTweet,
                                        nestedBriefTweet)))));
  }

  private void processImageRecognition(Tweet tweet, BriefTweet briefTweet, List<String> imageUrls) {
    imageRecognitionProcessors.forEach(
        recognitionProcessor ->
            processingExecutor.execute(
                () ->
                    imageUrls.forEach(
                        url ->
                            processingExecutor.execute(
                                () ->
                                    recognitionProcessingWrapper(
                                        recognitionProcessor, tweet, url, briefTweet, null)))));
  }

  private void recognitionProcessingWrapper(
      RecognitionProcessor recognitionProcessor,
      Tweet tweet,
      String url,
      BriefTweet briefTweet,
      BriefTweet nestedBriefTweet) {
    try {
      Recognition recognition = recognitionProcessor.processUrl(url);

      recognition.setTweetType(tweet.getType());
      recognition.setBriefTweet(briefTweet);
      recognition.setNestedBriefTweet(nestedBriefTweet);

      processingExecutor.execute(() -> processCommonTargetRecognition(recognition));
      processingExecutor.execute(() -> processLiveReleaseTargetRecognition(recognition));
    } catch (Exception ignored) {
    }
  }

  private void processCommonTarget(Tweet tweet) {
    if (isCommonTargetValid(tweet.getUser())) {
      kafkaProducer.sendTweetToBaseBroadcast(tweet);
    }
  }

  private void processLiveReleaseTarget(Tweet tweet) {
    if (isLiveReleaseTargetValid(tweet.getUser())) {
      kafkaProducer.sendTweetToLiveReleaseBroadcast(tweet);
    }
  }

  private void processCommonTargetRecognition(Recognition recognition) {
    if (isCommonTargetValid(recognition.getBriefTweet().getUser())) {
      kafkaProducer.sendRecognitionToBaseBroadcast(recognition);
    }
  }

  private void processLiveReleaseTargetRecognition(Recognition recognition) {
    if (isLiveReleaseTargetValid(recognition.getBriefTweet().getUser())) {
      kafkaProducer.sendRecognitionToLiveReleaseBroadcast(recognition);
    }
  }

  protected Boolean isCommonTargetValid(TwitterUser user) {
    return twitterHelperService.checkBasePass(user);
  }

  protected Boolean isLiveReleaseTargetValid(TwitterUser user) {
    return twitterHelperService.checkLiveReleasePass(user);
  }
}

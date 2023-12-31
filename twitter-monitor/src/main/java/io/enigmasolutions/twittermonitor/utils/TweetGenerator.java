package io.enigmasolutions.twittermonitor.utils;

import io.enigmasolutions.broadcastmodels.BriefTweet;
import io.enigmasolutions.broadcastmodels.Media;
import io.enigmasolutions.broadcastmodels.MediaType;
import io.enigmasolutions.broadcastmodels.Tweet;
import io.enigmasolutions.broadcastmodels.TwitterUser;
import io.enigmasolutions.twittermonitor.models.twitter.base.Entity;
import io.enigmasolutions.twittermonitor.models.twitter.base.ExtendedEntity;
import io.enigmasolutions.twittermonitor.models.twitter.base.TweetResponse;
import io.enigmasolutions.twittermonitor.models.twitter.base.User;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TweetGenerator {

  private TweetGenerator() {}

  public static Tweet generate(TweetResponse tweetResponse) {
    List<Media> media = retrieveMedia(tweetResponse);
    List<String> detectedUrls = retrieveUrls(tweetResponse);

    Tweet tweet = buildTweet(tweetResponse, media, detectedUrls);

    clearDuplicateMedia(tweet);

    return tweet;
  }

  private static Tweet buildTweet(
      TweetResponse tweetResponse, List<Media> media, List<String> detectedUrls) {
    Tweet.TweetBuilder tweetBuilder =
        Tweet.builder()
            .type(tweetResponse.getType())
            .user(buildTweetUser(tweetResponse.getUser()))
            .tweetUrl(tweetResponse.getTweetUrl())
            .media(media)
            .detectedUrls(detectedUrls)
            .followsUrl(tweetResponse.getFollowsUrl())
            .likesUrl(tweetResponse.getLikesUrl())
            .retweetsUrl(tweetResponse.getRetweetsUrl());

    if (tweetResponse.getText() != null) {
      tweetBuilder.text(tweetResponse.getText().trim());
    }

    if (tweetResponse.getRetweetedStatus() != null) {
      tweetBuilder.retweeted(generate(tweetResponse.getRetweetedStatus()));
    } else if (tweetResponse.getQuotedStatus() != null) {
      tweetBuilder.retweeted(generate(tweetResponse.getQuotedStatus()));
    } else if (tweetResponse.getInReplyToStatusId() != null) {
      tweetBuilder.replied(buildBriefTweet(tweetResponse.getRepliedStatus()));
    }

    return tweetBuilder.build();
  }

  public static BriefTweet buildBriefTweet(TweetResponse tweetResponse) {
    return BriefTweet.builder()
        .user(buildTweetUser(tweetResponse.getUser()))
        .tweetUrl(tweetResponse.getTweetUrl())
        .build();
  }

  private static List<Media> retrieveMedia(TweetResponse tweetResponse) {
    ExtendedEntity extendedEntities = tweetResponse.getExtendedEntities();
    if (extendedEntities == null) {
      return Collections.emptyList();
    }

    return extendedEntities.getMedia().stream()
        .map(
            media -> {
              String tweetResponseText = tweetResponse.getText();
              tweetResponse.setText(tweetResponseText.replace(media.getUrl(), ""));

              Media.MediaBuilder mediaBuilder =
                  Media.builder()
                      .type(MediaType.valueOf(media.getType().toUpperCase()))
                      .statical(media.getMediaUrl());

              if (media.getVideoInfo() != null) {
                String url = media.getVideoInfo().getVariants().get(0).getUrl();

                mediaBuilder.animation(url);
              }

              return mediaBuilder.build();
            })
        .collect(Collectors.toList());
  }

  private static List<String> retrieveUrls(TweetResponse tweetResponse) {
    Entity entity = tweetResponse.getEntities();
    if (entity == null) {
      return Collections.emptyList();
    }

    return entity.getUrls().stream()
        .map(
            url -> {
              String tweetResponseText = tweetResponse.getText();
              tweetResponse.setText(tweetResponseText.replace(url.getUrl(), url.getExpandedUrl()));

              return url.getExpandedUrl();
            })
        .collect(Collectors.toList());
  }

  public static TwitterUser buildTweetUser(User user) {
    return TwitterUser.builder()
        .updateTypes(user.getUpdateTypes())
        .name(user.getName())
        .icon(user.getUserImage())
        .login(user.getScreenName())
        .url(user.getUserUrl())
        .statusUrl(user.getUrl())
        .description(user.getDescription())
        .location(user.getLocation())
        .id(user.getId())
        .build();
  }

  private static void clearDuplicateMedia(Tweet tweet) {
    Tweet retweeted = tweet.getRetweeted();
    if (retweeted == null) {
      return;
    }

    if (tweet.getMedia().equals(retweeted.getMedia())) {
      tweet.setMedia(Collections.emptyList());
    }
  }
}

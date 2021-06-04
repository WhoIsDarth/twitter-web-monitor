package io.enigmarobotics.discordbroadcastservice.dto.wrappers;

import lombok.Data;

@Data
public class Tweet {

    private TweetType tweetType;
    private String text;
    private String userName;
    private String userId;
    private String userIcon;
    private String userUrl;
    private Tweet retweeted;
    private Tweet replied;
    private String image;
    private String tweetUrl;
    private String retweetsUrl;
    private String likesUrl;
    private String followsUrl;
}

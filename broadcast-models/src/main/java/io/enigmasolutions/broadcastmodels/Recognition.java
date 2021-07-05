package io.enigmasolutions.broadcastmodels;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Recognition {
    private TweetType tweetType;
    private RecognitionType recognitionType;
    private String userName;
    private String nestedUserName;
    private String source;
    private String result;
}

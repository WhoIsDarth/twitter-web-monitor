package io.enigmasolutions.twittermonitor.models.twitter.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @JsonProperty("screen_name")
    private String screenName;
    @JsonProperty("id_str")
    private String id;
    private String name;
    @JsonProperty("profile_image_url_https")
    private String userImage;
    private String userUrl;
    @JsonProperty("protected")
    private Boolean isProtected;

    public String getUserUrl() {
        return "https://twitter.com/" + screenName;
    }
}

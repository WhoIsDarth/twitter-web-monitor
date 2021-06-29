package io.enigmasolutions.webmonitor.webbroadcastservice.services.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.enigmasolutions.broadcastmodels.Tweet;
import io.enigmasolutions.webmonitor.webbroadcastservice.models.Timeline;
import io.enigmasolutions.webmonitor.webbroadcastservice.services.BroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import static io.enigmasolutions.webmonitor.webbroadcastservice.models.Timeline.TWITTER;
import static org.springframework.kafka.support.KafkaHeaders.TIMESTAMP;

@Slf4j
@Service
public class TweetConsumerService extends AbstractConsumerService<Tweet> {

    private final static Timeline TIMELINE = TWITTER;

    public TweetConsumerService(BroadcastService broadcastService) {
        super(Tweet.class, TIMELINE, broadcastService);
    }

    @Override
    @KafkaListener(topics = "${kafka.tweet-consumer.topic}",
            groupId = "${kafka.tweet-consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(
            @Payload String data,
            @Header(value = TIMESTAMP) String timestamp
    ) throws JsonProcessingException {
        super.consume(data, timestamp);
    }
}

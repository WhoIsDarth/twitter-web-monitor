package io.enigmarobotics.discordbroadcastservice.services.kafka;

import io.enigmarobotics.discordbroadcastservice.configuration.DiscordEmbedColorConfig;
import io.enigmarobotics.discordbroadcastservice.domain.models.Embed;
import io.enigmarobotics.discordbroadcastservice.domain.models.Message;
import io.enigmarobotics.discordbroadcastservice.services.PostmanService;
import io.enigmarobotics.discordbroadcastservice.utils.DiscordUtils;
import io.enigmasolutions.broadcastmodels.Alert;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlertConsumerService {

  private final static ExecutorService PROCESSING_EXECUTOR = Executors.newCachedThreadPool();

  private final PostmanService postmanService;
  private final DiscordEmbedColorConfig discordEmbedColorConfig;

  @Autowired
  AlertConsumerService(PostmanService postmanService,
      DiscordEmbedColorConfig discordEmbedColorConfig) {
    this.postmanService = postmanService;
    this.discordEmbedColorConfig = discordEmbedColorConfig;
  }

  @KafkaListener(topics = "${kafka.alert-consumer.topic}",
      groupId = "${kafka.alert-consumer.group-id}",
      containerFactory = "alertKafkaListenerContainerFactory")
  public void consume(Alert alert) {
    log.info("Received alert: {}", alert);

    PROCESSING_EXECUTOR.execute(() -> {
      Embed alertEmbed = DiscordUtils.generateAlertEmbed(alert, discordEmbedColorConfig.getAlert());
      Message message = Message.builder()
          .content("")
          .embeds(Collections.singletonList(alertEmbed))
          .build();
      postmanService.sendAlertEmbed(message);
    });
  }
}

package io.enigmasolutions.webmonitor.dictionaryservice.services;

import io.enigmasolutions.dictionarymodels.CustomerDiscordBroadcast;
import io.enigmasolutions.dictionarymodels.CustomerDiscordBroadcastConfig;
import io.enigmasolutions.dictionarymodels.CustomerDiscordGuild;
import io.enigmasolutions.dictionarymodels.CustomerTheme;
import io.enigmasolutions.webmonitor.dictionaryservice.db.models.documents.Customer;
import io.enigmasolutions.webmonitor.dictionaryservice.db.repositories.CustomerRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomerService {

  private final CustomerRepository customerRepository;

  public CustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public Mono<CustomerDiscordGuild> retrieveCustomerGuildDetails(String id) {
    return customerRepository
        .findById(id)
        .flatMap(
            customer -> {
              CustomerDiscordGuild customerDiscordGuild =
                  CustomerDiscordGuild.builder()
                      .guildId(customer.getDiscordGuild().getGuildId())
                      .moderatorsRoles(customer.getDiscordGuild().getModeratorsRoles())
                      .usersRoles(customer.getDiscordGuild().getUsersRoles())
                      .build();

              return Mono.just(customerDiscordGuild);
            });
  }

  public Mono<List<CustomerDiscordGuild>> retrieveAllCustomersGuildDetails() {
    return customerRepository
        .findAll()
        .flatMap(
            customer -> {
              CustomerDiscordGuild customerDiscordGuild =
                  CustomerDiscordGuild.builder()
                      .customerId(customer.getId())
                      .channelId(customer.getDiscordGuild().getChannelId())
                      .guildId(customer.getDiscordGuild().getGuildId())
                      .moderatorsRoles(customer.getDiscordGuild().getModeratorsRoles())
                      .usersRoles(customer.getDiscordGuild().getUsersRoles())
                      .build();

              return Mono.just(customerDiscordGuild);
            })
        .collect(Collectors.toList());
  }

  public Mono<CustomerTheme> retrieveCustomerTheme(String id) {
    return customerRepository
        .findById(id)
        .flatMap(
            customer -> {
              CustomerTheme customerTheme =
                  CustomerTheme.builder()
                      .generalColor(customer.getTheme().getGeneralColor())
                      .logoUrl(customer.getTheme().getLogoUrl())
                      .build();

              return Mono.just(customerTheme);
            });
  }

  public Mono<List<CustomerDiscordBroadcastConfig>> retrieveAllCustomersDiscordBroadcastConfigs() {
    return customerRepository
        .findAll()
        .flatMap(
            customer -> {
              CustomerDiscordBroadcastConfig customerDiscordBroadcastConfig =
                  CustomerDiscordBroadcastConfig.builder()
                      .customerDiscordBroadcast(
                          CustomerDiscordBroadcast.builder()
                              .baseCustomerWebhooks(customer.getDiscordBroadcast().getBaseCustomerWebhooks())
                              .staffBaseCustomerWebhooks(
                                  customer.getDiscordBroadcast().getStaffBaseCustomerWebhooks())
                              .liveCustomerWebhooks(customer.getDiscordBroadcast().getLiveCustomerWebhooks())
                              .build())
                      .theme(
                          CustomerTheme.builder()
                              .tweetColor(customer.getTheme().getTweetColor())
                              .retweetColor(customer.getTheme().getRetweetColor())
                              .replyColor(customer.getTheme().getReplyColor())
                              .isCustom(customer.getTheme().getIsCustom())
                              .generalColor(customer.getTheme().getGeneralColor())
                              .logoUrl(customer.getTheme().getLogoUrl())
                              .build())
                      .build();

              return Mono.just(customerDiscordBroadcastConfig);
            })
        .collect(Collectors.toList());
  }

  public Mono<Customer> changeDiscordGuildsChannelId(String guildId, String channelId) {
    return customerRepository
        .findCustomerByDiscordGuild_GuildId(guildId)
        .flatMap(
            customer -> {
              customer.getDiscordGuild().setChannelId(channelId);
              return customerRepository.save(customer);
            });
  }
}

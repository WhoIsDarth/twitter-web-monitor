package io.enigmasolutions.staffmanagerbot.services;

import io.enigmasolutions.dictionarymodels.CustomerDiscordGuild;
import io.enigmasolutions.staffmanagerbot.services.web.DictionaryClient;
import java.util.List;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CustomerDiscordGuildCache {

  private final DictionaryClient dictionaryClient;

  public CustomerDiscordGuildCache(DictionaryClient dictionaryClient) {
    this.dictionaryClient = dictionaryClient;
  }

  @Cacheable(value = "customers")
  public List<CustomerDiscordGuild> getCustomers() {
    return dictionaryClient.getCustomerDiscordGuilds();
  }

  @CachePut(value = "customers")
  public List<CustomerDiscordGuild> updateCustomers() {
    return getCustomers();
  }
}

package io.enigmasolutions.twittermonitor.controllers;

import io.enigmasolutions.twittermonitor.db.models.TwitterConsumer;
import io.enigmasolutions.twittermonitor.db.models.User;
import io.enigmasolutions.twittermonitor.db.repositories.TwitterConsumerRepository;
import io.enigmasolutions.twittermonitor.models.monitor.UserStartForm;
import io.enigmasolutions.twittermonitor.services.TwitterHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/config")
public class MonitorConfigurationController {

    private TwitterHelperService twitterHelperService;
    private TwitterConsumerRepository twitterConsumerRepository;

    @Autowired
    public MonitorConfigurationController(TwitterHelperService twitterHelperService, TwitterConsumerRepository twitterConsumerRepository) {
        this.twitterHelperService = twitterHelperService;
        this.twitterConsumerRepository = twitterConsumerRepository;
    }

    @PostMapping("/consumers")
    public void createConsumer(@RequestBody TwitterConsumer consumer){

        twitterConsumerRepository.insert(consumer);
    }

    @GetMapping("/consumers")
    public List<TwitterConsumer> getConsumers(){
        return twitterConsumerRepository.findAll();
    }

    @PatchMapping("/consumers")
    public void updateConsumer(){

    }

    @DeleteMapping("/consumers")
    public void deleteConsumer(@RequestBody TwitterConsumer twitterConsumer){

        String id = twitterConsumer.getId();

        twitterConsumerRepository.deleteById(id);
    }


}

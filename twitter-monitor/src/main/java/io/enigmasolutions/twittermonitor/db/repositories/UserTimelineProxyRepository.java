package io.enigmasolutions.twittermonitor.db.repositories;

import io.enigmasolutions.twittermonitor.db.models.documents.UserTimelineProxy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTimelineProxyRepository extends MongoRepository<UserTimelineProxy, String> {
}

package com.vprokopiv.ytbot.state;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StateRepository extends CrudRepository<StateEntry, String> {
    Optional<StateEntry> findByKey(String key);
}

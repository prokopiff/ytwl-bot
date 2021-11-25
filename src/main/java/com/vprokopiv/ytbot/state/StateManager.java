package com.vprokopiv.ytbot.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StateManager {
    private final StateRepository stateRepository;
    private final ObjectMapper objectMapper;

    public StateManager(StateRepository stateRepository, ObjectMapper objectMapper) {
        this.stateRepository = stateRepository;
        this.objectMapper = objectMapper;
    }


    public Optional<String> getState(String key) {
        return stateRepository.findByKey(key).map(StateEntry::getValue);
    }

    public void setState(String key, String value) {
        stateRepository.save(new StateEntry(key, value));
    }

    public void setState(String key, Object value) {
        String str;
        try {
            str = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        setState(key, str);
    }

    public <T> Optional<T> getState(String key, TypeReference<T> typ) {
        return stateRepository.findByKey(key)
                .map(StateEntry::getValue)
                .map(str -> {
                    try {
                        return objectMapper.readValue(str, typ);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}

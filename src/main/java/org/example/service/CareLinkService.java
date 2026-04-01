package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.repository.CareLinkRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareLinkService {

    private final CareLinkRepository careLinkRepository;
    private final UserRepository userRepository;


}

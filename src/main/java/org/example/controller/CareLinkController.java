package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.CareLinkService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/careLinks")
public class CareLinkController {

    private final CareLinkService careLinkService;


}

package com.github.gsmpro.brokerage.utils;

import lombok.experimental.UtilityClass;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@UtilityClass
public class Utils {
    public String readFileToString(String filename) throws Exception {
        return Files.lines(Paths.get(filename)).collect(Collectors.joining());
    }
}
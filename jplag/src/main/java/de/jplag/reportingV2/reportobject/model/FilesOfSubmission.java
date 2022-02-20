package de.jplag.reportingV2.reportobject.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FilesOfSubmission {

    @JsonProperty("file_name")
    private final String fileName;

    @JsonProperty("lines")
    private final List<String> lines;

    public FilesOfSubmission(String fileName, List<String> file_code) {
        this.fileName = fileName;
        this.lines = List.copyOf(file_code);
    }

    public String getFileName() {
        return fileName;
    }

    public List<String> getLines() {
        return lines;
    }

}

package com.rzagorski.retrofitrxerrorhandler.utils;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.rzagorski.retrofitrxerrorhandler.model.Repository;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by Robert Zagórski on 2016-09-29.
 */

public class FileUtils {

    public String loadJSON(String fileName, Type outputType) throws FileNotFoundException, IOException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(fileName));
        List<Repository> repositoryList = gson.fromJson(reader, outputType);
        reader.close();
        return gson.toJson(repositoryList);
    }

    public void writeJSON(String json, String fileName) throws UnsupportedEncodingException, FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileName, "UTF-8");
        writer.println(json);
        writer.close();
    }
}

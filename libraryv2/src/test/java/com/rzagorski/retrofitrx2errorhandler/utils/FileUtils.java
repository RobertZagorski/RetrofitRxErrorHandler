/*
 * Copyright (C) 2016 Robert Zagórski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rzagorski.retrofitrx2errorhandler.utils;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.rzagorski.retrofitrx2errorhandler.model.Repository;

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

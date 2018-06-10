package com.tudresden.navigationrobot;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.LinkedList;

/**
 * This class handles the storing an retrieving of data.
 *
 * @author Nadja Konrad
 */
public class StorageHelper {

    /**
     * The name of the file that the positions are stored in.
     */
    private static final String FILENAME = "positions.json";

    /**
     * The Gson instance for serialization and deserialization.
     */
    private Gson mGson = new Gson();

    /**
     * The Type LinkedList<Position> that is needed for serialization and deserialization with Gson.
     */
    private Type mListType = new TypeToken<LinkedList<Position>>(){}.getType();

    /**
     * The application context.
     */
    private Context mContext;

    public StorageHelper(Context context) {
        this.mContext = context;
    }

    /**
     * Checks whether the file with the filename positions.json already exists.
     * @return true if the file already exists; false otherwise
     */
    public boolean fileExists() {
        String path = mContext.getFilesDir().getAbsolutePath() + "/" + FILENAME;
        File file = new File(path);
        return file.exists();
    }

    /**
     * Serializes a list of positions with Gson and writes the resulting String to the file
     * positions.json in the internal storage.
     */
    public void storePositions(LinkedList<Position> positions) {
        String json = mGson.toJson(positions, mListType);
        try {
            FileOutputStream fileOutputStream = mContext.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            if(json != null) {
                fileOutputStream.write(json.getBytes());
            }
            fileOutputStream.close();
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "File not found!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reads the content of a file.
     * @return a String containing the content of the file
     */
    public String retrieveContent() {
        try {
            FileInputStream fileInputStream = mContext.openFileInput(FILENAME);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "File not found!", Toast.LENGTH_SHORT).show();
            return "Error!";
        }
    }

    /**
     * Converts a JSON String representation of a LinkedList<Position> to an actual LinkedList<Position>
     * @return a LinkedList<Position>
     */
    public LinkedList<Position> convertPositions() {
        return mGson.fromJson(retrieveContent(), mListType);
    }

}

/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.util.regex.*;

public class PastebinPoster {
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;
    
    public static void paste(String code, PasteCallback callback) {
        PasteProcessor processor = new PasteProcessor(code, callback);
        Thread thread = new Thread(processor);
        thread.start();
    }

    public static interface PasteCallback {
        public void handleSuccess(String url);
        public void handleError(String err);
    }
    
    private static class PasteProcessor implements Runnable {
        private String code;
        private PasteCallback callback;
        
        public PasteProcessor(String code, PasteCallback callback) {
            this.code = code;
            this.callback = callback;
        }
        
        @Override
        public void run() {
            HttpURLConnection conn = null;
            OutputStream out = null; 
            InputStream in = null;
            
            try {
                URL url = new URL("http://hastebin.prwn.net/documents");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setRequestMethod("POST");
                conn.addRequestProperty("Content-type", "text/plain");
                conn.setInstanceFollowRedirects(false);
                conn.setDoOutput(true);
                out = conn.getOutputStream();
                
                // code to POST
                out.write(code.getBytes());
                out.flush();
                out.close();
                
                if (conn.getResponseCode() == 200) {     
                    in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                        response.append("\r\n");
                    }
                    reader.close();
                    
                    String result = response.toString().trim();
                    // {"key":"abcde"}
                    if (result.matches("^.*key.*")) {
                        Pattern pattern = Pattern.compile("\"key\":\"(.*?)\"");
                        Matcher key = pattern.matcher(result);
                        while (key.find()) {
                            callback.handleSuccess("http://hastebin.prwn.net/" + key.group(1));
                        }
                        //callback.handleSuccess(result.trim());
                    } else {
                        String err = result.trim();
                        if (err.length() > 100) {
                            err = err.substring(0, 100);
                        }
                        callback.handleError(err);
                    }
                } else {
                    callback.handleError("An error occurred while uploading the text.");
                }
            } catch (IOException e) {
                callback.handleError(e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        
    }
    
}

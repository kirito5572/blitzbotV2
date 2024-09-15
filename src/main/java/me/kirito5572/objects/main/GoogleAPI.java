package me.kirito5572.objects.main;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.validator.routines.UrlValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleAPI {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAPI.class);
    private final String Key;
    /** @noinspection unused*/
    public GoogleAPI(String Key) {
        this.Key = Key;

    }
    @Nullable
    public String[][] Search(@NotNull String name) {
        try {
            String[][] returns = new String[10][2];
            String apiURL = "https://www.googleapis.com/youtube/v3/search";
            apiURL += "?key=" + Key;
            apiURL += "&part=snippet&type=video&maxResults=10&order=relevance&safeSearch=none";
            apiURL += "&q=" + URLEncoder.encode(name, StandardCharsets.UTF_8);

            URL url = new URI(apiURL).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            System.out.println(url);
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            JsonElement element = JsonParser.parseString(response.toString());
            JsonArray items = element.getAsJsonObject().get("items").getAsJsonArray();
            try {
                for(int i = 0; i < 10; i++) {
                    returns[i][0] = items.get(i).getAsJsonObject().get("snippet").getAsJsonObject().get("title").getAsString();
                    returns[i][1] = items.get(i).getAsJsonObject().get("id").getAsJsonObject().get("videoId").getAsString();
                }
            } catch (Exception e) {
                logger.error("youtube api error(parse data)");
                logger.info(e.getMessage());
                e.fillInStackTrace();
            }
            return returns;

        } catch (IOException | URISyntaxException e) {
            logger.error("youtube api error(get data)");
            logger.info(e.getMessage());
            e.fillInStackTrace();
        }
        return null;
    }

    private String translator(String input) {
        return TranslateOptions.newBuilder().setQuotaProjectId("youtube-search-api-362813").build().getService()
                .translate(
                        input,
                        Translate.TranslateOption.sourceLanguage("en"),
                        Translate.TranslateOption.targetLanguage("ko"),
                        Translate.TranslateOption.format("text")
                ).getTranslatedText();
    }

    private String translationInputExceptionHandling (String input, Guild guild) {
        Role role = null;
        if(guild.getId().equals("826704284003205160")) {
            role = guild.getRoleById("1045683935872552980"); //자동 번역 알림 역할
        }
        String output = null;
        String[] handlingWord = new String[] {
                "@here", "@everyone"
        };

        //특정 단어 제외
        boolean isTranslate = false;
        for(String word : handlingWord) {
            if(input.contains(word)) {
                if(role != null)
                    output = role.getAsMention() + translator(input.replace(word, ""));
                else
                    output = translator(input.replace(word, ""));
                isTranslate = true;
            }
        }

        //url 제외
        String url = urlChecker(input);
        if(url != null) {
            String replaceUrl = input.replace(url, "");
            output = translator(replaceUrl) + url;
            isTranslate = true;
        }
        if(!isTranslate) {
            output = translator(input);
        }

        return output;
    }

    private String urlChecker (@NotNull String input) {
        UrlValidator urlValidator = new UrlValidator();
        String url = extractUrl(input);
        if(url != null)
            if(urlValidator.isValid(url))
                return url;
            else
                return null;
        else
            return null;
    }

    private String extractUrl(String content){
        try {
            String REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
            Pattern p = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            if (m.find()) {
                return m.group();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String googleTranslateModule(String inputString, Guild guild) {
        String[] inputStringList = inputString.split("\n");
        StringBuilder builder = new StringBuilder();
        for(String input : inputStringList) {
            if(input.length() > 1) {
                builder.append(translationInputExceptionHandling(input, guild)).append("\n");
            } else {
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}
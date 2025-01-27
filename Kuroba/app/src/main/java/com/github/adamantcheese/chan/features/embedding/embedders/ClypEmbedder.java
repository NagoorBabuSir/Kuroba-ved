package com.github.adamantcheese.chan.features.embedding.embedders;

import static com.github.adamantcheese.chan.utils.BuildConfigUtils.AUDIO_THUMB_URL;
import static com.github.adamantcheese.chan.utils.StringUtils.prettyPrintDateUtilsElapsedTime;

import android.graphics.Bitmap;
import android.util.JsonReader;

import com.github.adamantcheese.chan.core.model.PostImage;
import com.github.adamantcheese.chan.core.net.NetUtilsClasses;
import com.github.adamantcheese.chan.core.repository.BitmapRepository;
import com.github.adamantcheese.chan.features.embedding.EmbedNoTitleException;
import com.github.adamantcheese.chan.features.embedding.EmbedResult;
import com.github.adamantcheese.chan.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;

public class ClypEmbedder
        extends JsonEmbedder {
    private static final Pattern CLYP_LINK_PATTERN = Pattern.compile("https?://clyp.it/(\\w{8})(?:/|\\b)");

    @Override
    public boolean shouldEmbed(CharSequence comment) {
        return StringUtils.containsAny(comment, "clyp.it");
    }

    @Override
    public boolean shouldCacheResults() {
        return false; // links generated by this embedder have an expiration time
    }

    @Override
    public Bitmap getIconBitmap() {
        return BitmapRepository.clypIcon;
    }

    @Override
    public Pattern getEmbedReplacePattern() {
        return CLYP_LINK_PATTERN;
    }

    @Override
    public HttpUrl generateRequestURL(Matcher matcher) {
        return HttpUrl.get("https://api.clyp.it/" + matcher.group(1));
    }

    /* EXAMPLE JSON
    {
      "Status": "DownloadDisabled",
      "CommentsEnabled": true,
      "Category": "None",
      "AudioFileId": "j42441xr",
      "Title": "ob6 + piano + bigsky",
      "Description": "first encounter with the big sky",
      "Duration": 67.709,
      "Url": "https://clyp.it/j42441xr",
      "Mp3Url": "https://audio.clyp.it/j42441xr.mp3?Exp...",
      "SecureMp3Url": "https://audio.clyp.it/j42441xr.mp3?Exp...",
      "OggUrl": "https://audio.clyp.it/j42441xr.ogg?Exp...",
      "SecureOggUrl": "https://audio.clyp.it/j42441xr.ogg?Exp...",
      "DateCreated": "2020-09-20T05:16:29.473Z"
    }
     */

    @Override
    public NetUtilsClasses.Converter<EmbedResult, JsonReader> getInternalConverter() {
        return input -> {
            String title = null;
            double duration = Double.NaN;

            HttpUrl mp3Url = AUDIO_THUMB_URL;
            String fileId = "";

            input.beginObject();
            while (input.hasNext()) {
                String name = input.nextName();
                switch (name) {
                    case "Title":
                        title = input.nextString();
                        break;
                    case "Duration":
                        duration = input.nextDouble();
                        break;
                    case "AudioFileId":
                        fileId = input.nextString();
                        break;
                    case "Mp3Url":
                        mp3Url = HttpUrl.get(input.nextString());
                        break;
                    default:
                        input.skipValue();
                        break;
                }
            }
            input.endObject();

            if (title == null) throw new EmbedNoTitleException();

            return new EmbedResult(
                    title,
                    prettyPrintDateUtilsElapsedTime(duration),
                    new PostImage.Builder()
                            .serverFilename(fileId)
                            .thumbnailUrl(HttpUrl.get(
                                    "https://static.clyp.it/site/images/favicons/apple-touch-icon-precomposed.png"))
                            .imageUrl(mp3Url)
                            .filename(title)
                            .extension("mp3")
                            .isInlined()
                            .build()
            );
        };
    }
}

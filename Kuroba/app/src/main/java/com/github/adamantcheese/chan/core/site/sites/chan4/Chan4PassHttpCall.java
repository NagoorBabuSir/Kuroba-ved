/*
 * Kuroba - *chan browser https://github.com/Adamantcheese/Kuroba/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.adamantcheese.chan.core.site.sites.chan4;

import static com.github.adamantcheese.chan.core.net.NetUtilsClasses.HTML_CONVERTER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.adamantcheese.chan.core.net.NetUtilsClasses;
import com.github.adamantcheese.chan.core.net.ProgressRequestBody;
import com.github.adamantcheese.chan.core.site.http.*;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.*;

public class Chan4PassHttpCall
        extends HttpCall<LoginResponse> {
    private final LoginRequest loginRequest;

    public Chan4PassHttpCall(
            @NonNull NetUtilsClasses.ResponseResult<LoginResponse> callback, LoginRequest loginRequest
    ) {
        super(callback);
        this.loginRequest = loginRequest;
    }

    @Override
    public void setup(
            Request.Builder requestBuilder, @Nullable ProgressRequestBody.ProgressRequestListener progressListener
    ) {
        FormBody.Builder formBuilder = new FormBody.Builder();

        // https://github.com/4chan/4chan-API/issues/91#issuecomment-855168518
        if (loginRequest.login) {
            formBuilder.add("id", loginRequest.user);
            formBuilder.add("pin", loginRequest.pass);
            // disabled as a result of 4chan expecting "long logins" to all be from the same IP address
            // which is fine on a computer with a relatively static IP, however doesn't work well on mobile
            // formBuilder.add("long_login", "1");
        } else {
            formBuilder.add("logout", "1");
        }

        requestBuilder.url(loginRequest.site.endpoints().login());
        requestBuilder.post(formBuilder.build());
    }

    @Override
    public LoginResponse convert(Response response) {
        if (!loginRequest.login) {
            return new LoginResponse("Logged out!", true);
        }

        String message = "Unknown error";
        try {
            Document document = HTML_CONVERTER.convert(response);
            Elements found = new Elements();
            found.addAll(document.select(".msg-error:not(.hidden)"));
            boolean success = found.addAll(document.select(".msg-success:not(.hidden)"));
            Element responseMessage = found.first();
            if (responseMessage != null) {
                message = responseMessage.text();
            }
            return new LoginResponse(message, success);
        } catch (Exception e) {
            return new LoginResponse(message, false);
        }
    }
}

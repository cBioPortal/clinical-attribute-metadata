/*
 * Copyright (c) 2018 - 2020 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center
 * has been advised of the possibility of such damage.
*/

package org.cbioportal.cdd.repository.topbraid;

import java.util.List;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 *
 * @author Manda Wilson
 **/
public class TopBraidSessionManager {

    private final static Logger logger = LoggerFactory.getLogger(TopBraidSessionConfiguration.class);

    private TopBraidSessionConfiguration configuration;

    private Cookie sessionIdCookie;

    public TopBraidSessionManager(TopBraidSessionConfiguration configuration) {
        this.configuration = configuration;
    }

    public TopBraidSessionConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(TopBraidSessionConfiguration configuration) {
        this.configuration = configuration;
    }

    /*
     * Get the session id, if has not been set then set it
     */
    public String getSessionId() {
        if (sessionIdCookie != null) {
            logger.debug("getSessionId() -- returning session id: " + sessionIdCookie.getValue());
            return sessionIdCookie.getValue();
        }
        logger.debug("getSessionId() -- session id is null, get a new one");
        return getFreshSessionId();
    }

    /*
     * Get a fresh session id
     */
    public String getFreshSessionId() {
        // we need a valid session id to query the login page, so first get that from another page
        Cookie initialSessionIdCookie = getSessionIdCookie(configuration.getLoginURL());
        if (initialSessionIdCookie != null) {
            logger.debug("getFreshSessionId() -- initial session id: " + initialSessionIdCookie.getValue());
            // now actually login, using our session id
//TODO : build URI with same approach as in repository classes (not as string)
            String loginURL = constructURL();
            // send our previous session id cookie and then replace it with the one attached to our successful login
            sessionIdCookie = getSessionIdCookie(loginURL, initialSessionIdCookie);
            if (sessionIdCookie != null) {
                logger.debug("getFreshSessionId() -- successfully logged in and session id is now: " + sessionIdCookie.getValue());
                return sessionIdCookie.getValue();
            }
            logger.error("getFreshSessionId() -- failed to login");
        } else {
            logger.debug("getFreshSessionId() -- failed to get initial session id.");
        }
        return null;
    }

    private Cookie getSessionIdCookie(String url) {
        return getSessionIdCookie(url, null);
    }

    private String constructURL() {
        return configuration.getLoginURL() + "/j_security_check" + "?" +
                "j_username=" + configuration.getUsername() + "&" +
                "j_password=" + configuration.getPassword();
    }

    private Cookie getSessionIdCookie(String url, Cookie initialSessionIdCookie) {
        try {
            HttpClientContext context = HttpClientContext.create();
            if (initialSessionIdCookie != null) {
                BasicCookieStore requestCookieStore = new BasicCookieStore();
                requestCookieStore.addCookie(initialSessionIdCookie);
                context.setCookieStore(requestCookieStore);
            }

            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(new HttpHead(url), context);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.OK.value()) {
                logger.error("Response status: '" + statusLine + "'");
                return null;
            }

            // get the cookie
            List<Cookie> cookies = context.getCookieStore().getCookies();
            for (Cookie cookie : cookies) {
                logger.debug("Cookie name: '" + cookie.getName() + "' value: '" + cookie.getValue() + "'");
                if (cookie.getName().equals("JSESSIONID")) {
                    return cookie;
                }
            }

            // close stuff
            client.close();
            response.close();
        } catch (Exception e) {
            logger.error("Unable to secure connection: '" + e + "'");
        }
        return null;
    }

}

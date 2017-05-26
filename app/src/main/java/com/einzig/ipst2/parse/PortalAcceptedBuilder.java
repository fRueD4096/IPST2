/******************************************************************************
 *                                                                            *
 * Copyright 2017 Steven Foskett, Jimmy Ho, Ryan Porterfield                  *
 * Permission is hereby granted, free of charge, to any person obtaining a    *
 * copy of this software and associated documentation files (the "Software"), *
 * to deal in the Software without restriction, including without limitation  *
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,   *
 * and/or sell copies of the Software, and to permit persons to whom the      *
 * Software is furnished to do so, subject to the following conditions:       *
 *                                                                            *
 * The above copyright notice and this permission notice shall be included in *
 * all copies or substantial portions of the Software.                        *
 *                                                                            *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE*
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER     *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING    *
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER        *
 * DEALINGS IN THE SOFTWARE.                                                  *
 *                                                                            *
 ******************************************************************************/

package com.einzig.ipst2.parse;

import com.einzig.ipst2.database.DatabaseInterface;
import com.einzig.ipst2.portal.PortalAccepted;
import com.einzig.ipst2.portal.PortalSubmission;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ryan Porterfield
 * @since 2017-05-24
 */

final class PortalAcceptedBuilder extends PortalBuilder<PortalAccepted> {

    PortalAcceptedBuilder(DatabaseInterface db) {
        super(db);
    }

    @Override
    PortalAccepted build(String name, Date dateResponded, String message) {
        String pictureURL = parsePictureURL(message);
        Date dateSubmitted = dateResponded;
        PortalSubmission submission = findPortal(pictureURL);
        if (submission != null)
            dateSubmitted = submission.getDateSubmitted();
        String address = parseLiveAddress(message);
        String intelLink = parseIntelLink(message);
        return new PortalAccepted(name, dateSubmitted, pictureURL, dateResponded, address, intelLink);
    }

    /**
     * Parse the intel link for an accepted portal from the email.
     *
     * @param messageString The body of the email as a String for parsing.
     * @return Ingress Intel map link for the portal.
     */
    private String parseIntelLink(String messageString) {
        String intelLinkURL;
        Pattern p = Pattern.compile("href=\"(.*?)\"");
        Matcher m = p.matcher(messageString);
        if (m.find()) {
            intelLinkURL = m.group(1);
        }
        else
            intelLinkURL = "N/A";
        return intelLinkURL;
    }

    /**
     * Parse the address of an accepted portal from the email.
     *
     * @param messageString The body of the email as a String for parsing.
     * @return address of the portal.
     */
    private String parseLiveAddress(String messageString) {
        String liveAddress;
        Pattern titleFinder = Pattern.compile("<a[^>]*>(.*?)</a>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher regexMatcher = titleFinder.matcher(messageString);
        if (regexMatcher.find()) {
            liveAddress = regexMatcher.group(1);
        }
        else
            liveAddress = "N/A";
        return liveAddress;
    }
}

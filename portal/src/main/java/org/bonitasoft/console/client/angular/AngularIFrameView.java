/*******************************************************************************
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package org.bonitasoft.console.client.angular;

import static org.bonitasoft.web.toolkit.client.common.util.StringUtil.*;

import java.util.Arrays;
import java.util.List;

import org.bonitasoft.console.client.user.cases.view.IFrameView;
import org.bonitasoft.web.toolkit.client.common.TreeIndexed;
import org.bonitasoft.web.toolkit.client.common.url.UrlSerializer;
import org.bonitasoft.web.toolkit.client.eventbus.MainEventBus;
import org.bonitasoft.web.toolkit.client.eventbus.events.MenuClickEvent;
import org.bonitasoft.web.toolkit.client.eventbus.events.MenuClickHandler;
import org.bonitasoft.web.toolkit.client.ui.RawView;
import org.bonitasoft.web.toolkit.client.ui.component.core.UiComponent;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * @author Vincent Elcrin
 * @author Julien Reboul
 */
public class AngularIFrameView extends RawView {

    public static final String CASE_LISTING_ADMIN_TOKEN = "caselistingadmin";

    public static final String CASE_LISTING_ARCHIVED_TAB = "archived";

    public static final String CASE_LISTING_TAB_TOKEN = "_tab";

    public static final String CASE_LISTING_PROCESS_ID_TOKEN = "processId";

    protected static final List<String> ANGULAR_TOKENS = Arrays.asList(CASE_LISTING_ADMIN_TOKEN);

    private final IFrameView iframe = new IFrameView();

    private String url;

    private String token;

    public AngularIFrameView() {
        MainEventBus.getInstance().addHandler(MenuClickEvent.TYPE, new MenuClickHandler() {

            @Override
            public void onMenuClick(final MenuClickEvent menuClickEvent) {
                // remove angular parameters from url
                final AngularParameterCleaner angularParameterCleaner = new AngularParameterCleaner(menuClickEvent.getToken(), getHash());
                updateHash(angularParameterCleaner.getHashWithoutAngularParameters());
            }
        });
    }

    public native String getHash() /*-{
        return $wnd.location.hash;
    }-*/;

    public native void updateHash(String hash) /*-{
        $wnd.location.hash = hash;
    }-*/;

    @Override
    public String defineToken() {
        return null;
    }

    @Override
    public void buildView() {
        final SimplePanel panel = new SimplePanel();
        panel.setStyleName("body");
        panel.add(iframe);
        addBody(new UiComponent(panel));
        addClass("page page_custompage_");
    }

    @Override
    protected void refreshAll() {
    }

    /**
     * @param url
     *        Iframe url to set
     */
    public void setUrl(final String url, final String token) {
        setToken(token);
        this.url = url;
        this.token = token;
    }

    /**
     * build angular Url
     *
     * @param url
     *        the angular base path
     * @param token
     *        the current page token
     * @param queryString
     *        the URL query to set
     * @return the angular url to access for the given token
     */
    protected String buildAngularUrl(final String url, final String token, final String queryString) {
        return new AngularUrlBuilder(url)
        .appendQueryStringParameter(token + "_id", queryString + "&" + getHash())
        .appendQueryStringParameter(token + "_tab", queryString + "&" + getHash())
        .build() + (isBlank(queryString) ? "" : "?" + queryString.replaceAll(token + '_', ""));
    }

    /**
     * @return the token
     */
    @Override
    public String getToken() {
        return token;
    }

    public void display(final TreeIndexed<String> params) {
        iframe.setLocation(buildAngularUrl(url, token, UrlSerializer.serialize(params)));
    }

    /**
     * @param tokens
     * @return
     */
    public boolean isFormerTokenAnAngularFrame(final String tokens) {
        final MatchResult paramMatcher = RegExp.compile("(^|[&\\?#])_p=([^&\\?#]*)([&\\?#]|$)").exec(tokens);
        if (paramMatcher != null && paramMatcher.getGroupCount() > 0) {
            return ANGULAR_TOKENS.contains(paramMatcher.getGroup(2));
        }
        return false;
    }
}

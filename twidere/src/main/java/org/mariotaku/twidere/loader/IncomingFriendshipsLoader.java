/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.loader;

import android.content.Context;
import android.support.annotation.NonNull;

import org.mariotaku.twidere.api.MicroBlog;
import org.mariotaku.twidere.api.twitter.TwitterException;
import org.mariotaku.twidere.api.twitter.model.IDs;
import org.mariotaku.twidere.api.twitter.model.Paging;
import org.mariotaku.twidere.api.twitter.model.ResponseList;
import org.mariotaku.twidere.api.twitter.model.User;
import org.mariotaku.twidere.model.ParcelableAccount;
import org.mariotaku.twidere.model.ParcelableCredentials;
import org.mariotaku.twidere.model.ParcelableUser;
import org.mariotaku.twidere.model.UserKey;
import org.mariotaku.twidere.model.util.ParcelableAccountUtils;

import java.util.List;

public class IncomingFriendshipsLoader extends CursorSupportUsersLoader {

    public IncomingFriendshipsLoader(final Context context, final UserKey accountKey,
                                     final List<ParcelableUser> data, boolean fromUser) {
        super(context, accountKey, data, fromUser);
    }

    @NonNull
    @Override
    protected IDs getIDs(@NonNull final MicroBlog twitter, @NonNull ParcelableCredentials credentials, @NonNull final Paging paging) throws TwitterException {
        return twitter.getIncomingFriendships(paging);
    }

    @NonNull
    @Override
    protected ResponseList<User> getCursoredUsers(@NonNull MicroBlog twitter, @NonNull ParcelableCredentials credentials, @NonNull Paging paging) throws TwitterException {
        return twitter.getFriendshipsRequests(paging);
    }

    @Override
    protected boolean useIDs(@NonNull ParcelableCredentials credentials) {
        switch (ParcelableAccountUtils.getAccountType(credentials)) {
            case ParcelableAccount.Type.FANFOU: {
                return false;
            }
        }
        return true;
    }
}

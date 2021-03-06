/*
 *         Twidere - Twitter client for Android
 *
 * Copyright 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mariotaku.microblog.library.fanfou.api;

import org.mariotaku.microblog.library.MicroBlogException;
import org.mariotaku.microblog.library.twitter.model.Paging;
import org.mariotaku.microblog.library.twitter.model.ResponseList;
import org.mariotaku.microblog.library.twitter.model.Status;
import org.mariotaku.microblog.library.twitter.model.User;
import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.param.Query;

/**
 * Created by mariotaku on 16/3/10.
 */
public interface SearchResources {

    @GET("/search/public_timeline.json")
    ResponseList<Status> searchPublicTimeline(@Query("q") String query, @Query Paging paging) throws MicroBlogException;

    @GET("/search/users.json")
    ResponseList<User> searchFanfouUsers(@Query("q") String query, @Query Paging paging) throws MicroBlogException;

}

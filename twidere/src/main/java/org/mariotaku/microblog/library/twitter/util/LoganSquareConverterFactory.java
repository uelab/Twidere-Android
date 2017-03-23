/*
 *             Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.microblog.library.twitter.util;

import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;

import com.bluelinelabs.logansquare.JsonMapper;
import com.fasterxml.jackson.core.JsonParseException;

import org.mariotaku.commons.logansquare.LoganSquareMapperFinder;
import org.mariotaku.microblog.library.twitter.model.TwitterResponse;
import org.mariotaku.restfu.RestConverter;
import org.mariotaku.restfu.http.ContentType;
import org.mariotaku.restfu.http.HttpResponse;
import org.mariotaku.restfu.http.mime.Body;
import org.mariotaku.restfu.http.mime.SimpleBody;
import org.mariotaku.restfu.http.mime.StringBody;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by mariotaku on 2017/3/23.
 */

public class LoganSquareConverterFactory<E extends Exception> extends RestConverter.SimpleFactory<E> {

    protected SimpleArrayMap<Type, RestConverter<HttpResponse, ?, E>> responseConverters = new SimpleArrayMap<>();
    protected SimpleArrayMap<Type, RestConverter<?, Body, E>> sBodyConverters = new SimpleArrayMap<>();

    @Override
    public RestConverter<HttpResponse, ?, E> forResponse(Type type) throws RestConverter.ConvertException {
        RestConverter<HttpResponse, ?, E> converter = responseConverters.get(type);
        if (converter != null) {
            return converter;
        }
        final JsonMapper<?> mapper;
        try {
            mapper = LoganSquareMapperFinder.mapperFor(type);
        } catch (LoganSquareMapperFinder.ClassLoaderDeadLockException e) {
            throw new RestConverter.ConvertException(e);
        }
        return new JsonResponseConverter<>(mapper);
    }

    @Override
    public RestConverter<?, Body, E> forRequest(Type type) throws RestConverter.ConvertException {
        final RestConverter<?, Body, E> converter = sBodyConverters.get(type);
        if (converter != null) {
            return converter;
        }
        if (SimpleBody.supports(type)) {
            return new SimpleBodyConverter<>(type);
        }
        try {
            return new JsonRequestConverter<>(LoganSquareMapperFinder.mapperFor(type));
        } catch (LoganSquareMapperFinder.ClassLoaderDeadLockException e) {
            throw new RestConverter.ConvertException(e);
        }
    }

    @NonNull
    private static Object parseOrThrow(HttpResponse response, JsonMapper<?> mapper)
            throws IOException, RestConverter.ConvertException {
        try {
            final Object parsed = mapper.parse(response.getBody().stream());
            if (parsed == null) {
                throw new IOException("Empty data");
            }
            return parsed;
        } catch (JsonParseException e) {
            throw new RestConverter.ConvertException("Malformed JSON Data");
        }
    }

    private static class JsonResponseConverter<E extends Exception> implements RestConverter<HttpResponse, Object, E> {
        private final JsonMapper<?> mapper;

        JsonResponseConverter(JsonMapper<?> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Object convert(HttpResponse httpResponse) throws IOException, ConvertException, E {
            final Object object = parseOrThrow(httpResponse, mapper);
            if (object instanceof TwitterResponse) {
                ((TwitterResponse) object).processResponseHeader(httpResponse);
            }
            return object;
        }
    }

    private static class JsonRequestConverter<E extends Exception> implements RestConverter<Object, Body, E> {
        private final JsonMapper<Object> mapper;

        JsonRequestConverter(JsonMapper<Object> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Body convert(Object request) throws IOException, ConvertException, E {
            return new StringBody(mapper.serialize(request), ContentType.parse("application/json"));
        }
    }
}
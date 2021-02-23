/**
 * Copyright 2016 Mattias Karlsson
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streaminghost.radiosoundfm.services.metadata;

import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import okhttp3.CacheControl;
import okhttp3.Call;

public final class ShoutcastDataSourceFactory extends HttpDataSource.BaseFactory {

    private final Call.Factory callFactory;
    private final String userAgent;
    private final TransferListener transferListener;
    private final ShoutcastMetadataListener shoutcastMetadataListener;
    private final CacheControl cacheControl;

    public ShoutcastDataSourceFactory(Call.Factory callFactory, String userAgent,
                                      TransferListener transferListener,
                                      ShoutcastMetadataListener shoutcastMetadataListener) {
        this(callFactory, userAgent, transferListener, shoutcastMetadataListener, null);
    }

    private ShoutcastDataSourceFactory(Call.Factory callFactory, String userAgent,
                                       TransferListener transferListener,
                                       ShoutcastMetadataListener shoutcastMetadataListener, CacheControl cacheControl) {
        this.callFactory = callFactory;
        this.userAgent = userAgent;
        this.transferListener = transferListener;
        this.shoutcastMetadataListener = shoutcastMetadataListener;
        this.cacheControl = cacheControl;
    }

    @Override
    protected HttpDataSource createDataSourceInternal(HttpDataSource.RequestProperties requestProperties) {
        return new ShoutcastDataSource(callFactory, userAgent, null, transferListener, shoutcastMetadataListener, cacheControl);
    }

}

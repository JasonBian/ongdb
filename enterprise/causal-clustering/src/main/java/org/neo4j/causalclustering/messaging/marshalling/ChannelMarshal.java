/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.causalclustering.messaging.marshalling;

import java.io.IOException;

import org.neo4j.causalclustering.core.state.storage.SafeChannelMarshal;
import org.neo4j.causalclustering.messaging.EndOfStreamException;
import org.neo4j.storageengine.api.ReadableChannel;
import org.neo4j.storageengine.api.WritableChannel;

/**
 * Implementations of this class perform marshalling (encoding/decoding) of {@link STATE}
 * into/from a {@link WritableChannel} and a {@link ReadableChannel} respectively.
 *
 * N.B.: Implementations should prefer to extend {@link SafeChannelMarshal} to handle
 * {@link org.neo4j.storageengine.api.ReadPastEndException} correctly.
 *
 * @param <STATE> The class of objects supported by this marshal
 */
public interface ChannelMarshal<STATE>
{
    /**
     * Marshals the state into the channel.
     */
    void marshal( STATE state, WritableChannel channel ) throws IOException;

    /**
     * Unmarshals an instance of {@link STATE} from channel. If the channel does not have enough bytes
     * to fully read an instance then an {@link EndOfStreamException} must be thrown.
     *
     * N.B: The ReadableChannel is sort of broken in its implementation and throws
     * {@link org.neo4j.storageengine.api.ReadPastEndException} which is a subclass of IOException
     * and that is problematic since usually the case of reaching the end of a stream actually
     * requires handling distinct from that of arbitrary IOExceptions. Although it was possible
     * to catch that particular exception explicitly, you would not get compiler/IDE support
     * for making that apparent.
     */
    STATE unmarshal( ReadableChannel channel ) throws IOException, EndOfStreamException;
}

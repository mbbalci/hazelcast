/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client.impl.protocol.codec;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.builtin.*;

import java.util.ListIterator;

import static com.hazelcast.client.impl.protocol.ClientMessage.*;
import static com.hazelcast.client.impl.protocol.codec.builtin.FixedSizeTypesCodec.*;

/**
 * TODO DOC
 */
public final class ClientAuthenticationCustomCodec {
    //hex: 0x000300
    public static final int REQUEST_MESSAGE_TYPE = 768;
    //hex: 0x000301
    public static final int RESPONSE_MESSAGE_TYPE = 769;
    private static final int REQUEST_IS_OWNER_CONNECTION_FIELD_OFFSET = PARTITION_ID_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int REQUEST_SERIALIZATION_VERSION_FIELD_OFFSET = REQUEST_IS_OWNER_CONNECTION_FIELD_OFFSET + BOOLEAN_SIZE_IN_BYTES;
    private static final int REQUEST_PARTITION_COUNT_FIELD_OFFSET = REQUEST_SERIALIZATION_VERSION_FIELD_OFFSET + BYTE_SIZE_IN_BYTES;
    private static final int REQUEST_INITIAL_FRAME_SIZE = REQUEST_PARTITION_COUNT_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int RESPONSE_STATUS_FIELD_OFFSET = CORRELATION_ID_FIELD_OFFSET + LONG_SIZE_IN_BYTES;
    private static final int RESPONSE_SERIALIZATION_VERSION_FIELD_OFFSET = RESPONSE_STATUS_FIELD_OFFSET + BYTE_SIZE_IN_BYTES;
    private static final int RESPONSE_PARTITION_COUNT_FIELD_OFFSET = RESPONSE_SERIALIZATION_VERSION_FIELD_OFFSET + BYTE_SIZE_IN_BYTES;
    private static final int RESPONSE_INITIAL_FRAME_SIZE = RESPONSE_PARTITION_COUNT_FIELD_OFFSET + INT_SIZE_IN_BYTES;

    private ClientAuthenticationCustomCodec() {
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static class RequestParameters {

        /**
         * Secret byte array for authentication.
         */
        public com.hazelcast.nio.serialization.Data credentials;

        /**
         * Unique string identifying the connected client uniquely. This string is generated by the owner member server
         * on initial connection. When the client connects to a non-owner member it sets this field on the request.
         */
        public java.lang.String uuid;

        /**
         * Unique string identifying the server member uniquely.
         */
        public java.lang.String ownerUuid;

        /**
         * You must set this field to true while connecting to the owner member, otherwise set to false.
         */
        public boolean isOwnerConnection;

        /**
         * The type of the client. E.g. JAVA, CPP, CSHARP, etc.
         */
        public java.lang.String clientType;

        /**
         * client side supported version to inform server side
         */
        public byte serializationVersion;

        /**
         * The Hazelcast version of the client. (e.g. 3.7.2)
         */
        public java.lang.String clientHazelcastVersion;

        /**
         * the name of the client instance
         */
        public java.lang.String clientName;

        /**
         * User defined labels of the client instance
         */
        public java.util.List<java.lang.String> labels;

        /**
         * the expected partition count of the cluster. Checked on the server side when provided.
         * Authentication fails and 3:NOT_ALLOWED_IN_CLUSTER returned, in case of mismatch
         */
        public int partitionCount;

        /**
         * the expected id of the cluster. Checked on the server side when provided.
         * Authentication fails and 3:NOT_ALLOWED_IN_CLUSTER returned, in case of mismatch
         */
        public java.lang.String clusterId;
    }

    public static ClientMessage encodeRequest(com.hazelcast.nio.serialization.Data credentials, java.lang.String uuid, java.lang.String ownerUuid, boolean isOwnerConnection, java.lang.String clientType, byte serializationVersion, java.lang.String clientHazelcastVersion, java.lang.String clientName, java.util.Collection<java.lang.String> labels, int partitionCount, java.lang.String clusterId) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        clientMessage.setRetryable(true);
        clientMessage.setAcquiresResource(false);
        clientMessage.setOperationName("Client.AuthenticationCustom");
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[REQUEST_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, REQUEST_MESSAGE_TYPE);
        encodeBoolean(initialFrame.content, REQUEST_IS_OWNER_CONNECTION_FIELD_OFFSET, isOwnerConnection);
        encodeByte(initialFrame.content, REQUEST_SERIALIZATION_VERSION_FIELD_OFFSET, serializationVersion);
        encodeInt(initialFrame.content, REQUEST_PARTITION_COUNT_FIELD_OFFSET, partitionCount);
        clientMessage.add(initialFrame);
        DataCodec.encode(clientMessage, credentials);
        CodecUtil.encodeNullable(clientMessage, uuid, StringCodec::encode);
        CodecUtil.encodeNullable(clientMessage, ownerUuid, StringCodec::encode);
        StringCodec.encode(clientMessage, clientType);
        StringCodec.encode(clientMessage, clientHazelcastVersion);
        StringCodec.encode(clientMessage, clientName);
        ListMultiFrameCodec.encode(clientMessage, labels, StringCodec::encode);
        CodecUtil.encodeNullable(clientMessage, clusterId, StringCodec::encode);
        return clientMessage;
    }

    public static ClientAuthenticationCustomCodec.RequestParameters decodeRequest(ClientMessage clientMessage) {
        ListIterator<ClientMessage.Frame> iterator = clientMessage.listIterator();
        RequestParameters request = new RequestParameters();
        ClientMessage.Frame initialFrame = iterator.next();
        request.isOwnerConnection = decodeBoolean(initialFrame.content, REQUEST_IS_OWNER_CONNECTION_FIELD_OFFSET);
        request.serializationVersion = decodeByte(initialFrame.content, REQUEST_SERIALIZATION_VERSION_FIELD_OFFSET);
        request.partitionCount = decodeInt(initialFrame.content, REQUEST_PARTITION_COUNT_FIELD_OFFSET);
        request.credentials = DataCodec.decode(iterator);
        request.uuid = CodecUtil.decodeNullable(iterator, StringCodec::decode);
        request.ownerUuid = CodecUtil.decodeNullable(iterator, StringCodec::decode);
        request.clientType = StringCodec.decode(iterator);
        request.clientHazelcastVersion = StringCodec.decode(iterator);
        request.clientName = StringCodec.decode(iterator);
        request.labels = ListMultiFrameCodec.decode(iterator, StringCodec::decode);
        request.clusterId = CodecUtil.decodeNullable(iterator, StringCodec::decode);
        return request;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static class ResponseParameters {

        /**
         * TODO DOC
         */
        public byte status;

        /**
         * TODO DOC
         */
        public com.hazelcast.nio.Address address;

        /**
         * Unique string identifying the connected client uniquely. This string is generated by the owner member server
         * on initial connection. When the client connects to a non-owner member it sets this field on the request.
         */
        public java.lang.String uuid;

        /**
         * Unique string identifying the server member uniquely.
         */
        public java.lang.String ownerUuid;

        /**
         * client side supported version to inform server side
         */
        public byte serializationVersion;

        /**
         * TODO DOC
         */
        public java.lang.String serverHazelcastVersion;

        /**
         * TODO DOC
         */
        public java.util.List<com.hazelcast.cluster.Member> clientUnregisteredMembers;

        /**
         * the expected partition count of the cluster. Checked on the server side when provided.
         * Authentication fails and 3:NOT_ALLOWED_IN_CLUSTER returned, in case of mismatch
         */
        public int partitionCount;

        /**
         * the expected id of the cluster. Checked on the server side when provided.
         * Authentication fails and 3:NOT_ALLOWED_IN_CLUSTER returned, in case of mismatch
         */
        public java.lang.String clusterId;
    }

    public static ClientMessage encodeResponse(byte status, com.hazelcast.nio.Address address, java.lang.String uuid, java.lang.String ownerUuid, byte serializationVersion, java.lang.String serverHazelcastVersion, java.util.Collection<com.hazelcast.cluster.Member> clientUnregisteredMembers, int partitionCount, java.lang.String clusterId) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[RESPONSE_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, RESPONSE_MESSAGE_TYPE);
        clientMessage.add(initialFrame);

        encodeByte(initialFrame.content, RESPONSE_STATUS_FIELD_OFFSET, status);
        encodeByte(initialFrame.content, RESPONSE_SERIALIZATION_VERSION_FIELD_OFFSET, serializationVersion);
        encodeInt(initialFrame.content, RESPONSE_PARTITION_COUNT_FIELD_OFFSET, partitionCount);
        CodecUtil.encodeNullable(clientMessage, address, AddressCodec::encode);
        CodecUtil.encodeNullable(clientMessage, uuid, StringCodec::encode);
        CodecUtil.encodeNullable(clientMessage, ownerUuid, StringCodec::encode);
        StringCodec.encode(clientMessage, serverHazelcastVersion);
        ListMultiFrameCodec.encodeNullable(clientMessage, clientUnregisteredMembers, MemberCodec::encode);
        StringCodec.encode(clientMessage, clusterId);
        return clientMessage;
    }

    public static ClientAuthenticationCustomCodec.ResponseParameters decodeResponse(ClientMessage clientMessage) {
        ListIterator<ClientMessage.Frame> iterator = clientMessage.listIterator();
        ResponseParameters response = new ResponseParameters();
        ClientMessage.Frame initialFrame = iterator.next();
        response.status = decodeByte(initialFrame.content, RESPONSE_STATUS_FIELD_OFFSET);
        response.serializationVersion = decodeByte(initialFrame.content, RESPONSE_SERIALIZATION_VERSION_FIELD_OFFSET);
        response.partitionCount = decodeInt(initialFrame.content, RESPONSE_PARTITION_COUNT_FIELD_OFFSET);
        response.address = CodecUtil.decodeNullable(iterator, AddressCodec::decode);
        response.uuid = CodecUtil.decodeNullable(iterator, StringCodec::decode);
        response.ownerUuid = CodecUtil.decodeNullable(iterator, StringCodec::decode);
        response.serverHazelcastVersion = StringCodec.decode(iterator);
        response.clientUnregisteredMembers = ListMultiFrameCodec.decodeNullable(iterator, MemberCodec::decode);
        response.clusterId = StringCodec.decode(iterator);
        return response;
    }

}
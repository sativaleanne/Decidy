package com.decidy.decidy.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object ChoiceListSerializer : Serializer<ChoiceList> {
    override val defaultValue: ChoiceList = ChoiceList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ChoiceList {
        try {
            return ChoiceList.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: ChoiceList, output: OutputStream) {
        t.writeTo(output)
    }
}

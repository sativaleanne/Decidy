package com.decidy.decidy.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.choiceDataStore: DataStore<ChoiceList> by dataStore(
    fileName = "choice_list.pb",
    serializer = ChoiceListSerializer
)

object ChoiceStorage {
    fun read(context: Context): Flow<List<ChoicePersist>> {
        return context.choiceDataStore.data.map { choiceList ->
            choiceList.choicesList.map {
                ChoicePersist(label = it.label, weight = it.weight)
            }
        }
    }

    suspend fun save(context: Context, choices: List<ChoicePersist>) {
        context.choiceDataStore.updateData { current ->
            current.toBuilder().clearChoices().addAllChoices(
                choices.map {
                    ChoiceProto.newBuilder()
                        .setLabel(it.label)
                        .setWeight(it.weight)
                        .build()
                }
            ).build()
        }
    }

    suspend fun clear(context: Context) {
        context.choiceDataStore.updateData {
            it.toBuilder().clearChoices().build()
        }
    }
}

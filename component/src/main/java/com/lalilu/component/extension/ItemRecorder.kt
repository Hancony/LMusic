package com.lalilu.component.extension

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf

class LazyListRecordScope internal constructor(
    var recorder: ItemRecorder,
) {
    var lazyListScope: LazyListScope? = null
        internal set

    fun stickyHeaderWithRecord(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.(Int) -> Unit
    ) {
        lazyListScope?.let { scope ->
            recorder.record(key)
            scope.stickyHeader(
                key = key,
                contentType = contentType,
                content = content
            )
        }
    }

    fun itemWithRecord(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit
    ) {
        lazyListScope?.let { scope ->
            recorder.record(key)
            scope.item(
                key = key,
                contentType = contentType,
                content = content
            )
        }
    }

    inline fun <T : Any> itemsWithRecord(
        items: List<T>,
        noinline key: ((item: T) -> Any)? = null,
        noinline contentType: (item: T) -> Any? = { null },
        crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
    ) {
        lazyListScope?.let { scope ->
            recorder.recordAll(items.map { key?.invoke(it) })
            scope.items(
                items = items,
                key = key,
                contentType = contentType,
                itemContent = itemContent
            )
        }
    }

    inline fun <T> itemsIndexedWithRecord(
        items: List<T>,
        noinline key: ((index: Int, item: T) -> Any)? = null,
        crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
        crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
    ) {
        lazyListScope?.let { scope ->
            recorder.recordAll(items.mapIndexed { index, item -> key?.invoke(index, item) })
            scope.itemsIndexed(
                items = items,
                key = key,
                contentType = contentType,
                itemContent = itemContent
            )
        }
    }
}

class ItemRecorder {
    private val keys = mutableStateListOf<Any?>()
    private val scope = LazyListRecordScope(this)

    fun record(key: Any?) = this.keys.add(key)
    fun recordAll(keys: List<Any?>) = this.keys.addAll(keys)
    fun clear() = keys.clear()
    fun list() = keys

    internal fun startRecord(
        lazyListScope: LazyListScope,
        block: LazyListRecordScope.() -> Unit
    ) {
        clear()
        scope.lazyListScope = lazyListScope
        scope.block()
    }
}

fun LazyListScope.startRecord(
    recorder: ItemRecorder,
    block: LazyListRecordScope.() -> Unit
) {
    recorder.startRecord(
        lazyListScope = this,
        block = block
    )
}
package com.lalilu.lplayer.runtime

import com.lalilu.lplayer.extensions.add
import com.lalilu.lplayer.extensions.getNextOf
import com.lalilu.lplayer.extensions.getPreviousOf
import com.lalilu.lplayer.extensions.move
import com.lalilu.lplayer.extensions.removeAt
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Timer
import kotlin.concurrent.schedule

interface Runtime<T> {
    val songsIdsFlow: MutableStateFlow<List<String>>
    val playingIdFlow: MutableStateFlow<String?>
    val positionFlow: MutableStateFlow<Long>
    val isPlayingFlow: MutableStateFlow<Boolean>

    var getPosition: () -> Long
    var listener: Listener?
    var timer: Timer?

    fun getPlaying(): T?
    fun getItemById(mediaId: String?): T?
    fun getPreviousOf(item: T, cycle: Boolean): T?
    fun getNextOf(item: T, cycle: Boolean): T?
    fun getShuffle(): T?


    /**
     * 加载指定的歌曲进播放列表，更新当前正在播放的歌曲
     */
    fun load(
        songs: List<String> = emptyList(),
        playing: String?,
    ) {
        update(songs = songs)
        if (songs.contains(playing)) {
            update(playing = playing)
        }
    }

    /**
     * 更新当前正在播放的歌曲
     */
    fun update(playing: String? = null) {
        listener?.onPlayingUpdate(playing)
        playingIdFlow.value = playing
    }

    fun update(songs: List<String>) {
        listener?.onSongsUpdate(songs)
        songsIdsFlow.value = songs
    }

    fun update(isPlaying: Boolean) {
        listener?.onIsPlayingUpdate(isPlaying)
        isPlayingFlow.value = isPlaying
    }

    fun move(from: Int, to: Int) {
        update(songsIdsFlow.value.move(from, to))
    }

    fun add(index: Int = -1, song: String) {
        update(songsIdsFlow.value.add(index, song))
    }

    fun remove(mediaId: String) {
        removeAt(indexOfSong(mediaId))
    }

    fun removeAt(index: Int) {
        update(songsIdsFlow.value.removeAt(index))
    }

    fun isEmpty(): Boolean = songsIdsFlow.value.isEmpty()
    fun getPlayingId(): String? = playingIdFlow.value
    fun getPlayingIndex(): Int = songsIdsFlow.value.indexOf(playingIdFlow.value)
    fun indexOfSong(mediaId: String): Int = songsIdsFlow.value.indexOfFirst { it == mediaId }
    fun getNextOf(mediaId: String?, cycle: Boolean): String? =
        songsIdsFlow.value.getNextOf(mediaId, cycle)

    fun getPreviousOf(mediaId: String?, cycle: Boolean): String? =
        songsIdsFlow.value.getPreviousOf(mediaId, cycle)

    fun updatePosition(startPosition: Long, isPlaying: Boolean) {
        timer?.cancel()
        positionFlow.value = startPosition
        listener?.onPositionUpdate(positionFlow.value)

        if (!isPlaying) return
        timer = Timer().apply {
            schedule(0, 50L) {
                positionFlow.value = getPosition()
                listener?.onPositionUpdate(positionFlow.value)
            }
        }
    }

    interface Listener {
        fun onPlayingUpdate(songId: String?) {}
        fun onSongsUpdate(songsIds: List<String>) {}
        fun onPositionUpdate(position: Long) {}
        fun onIsPlayingUpdate(isPlaying: Boolean) {}
    }
}